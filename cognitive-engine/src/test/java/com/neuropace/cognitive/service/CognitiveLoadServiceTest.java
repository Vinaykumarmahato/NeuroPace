package com.neuropace.cognitive.service;

import com.neuropace.cognitive.entity.LearningEvent;
import com.neuropace.cognitive.entity.OverloadDetection;
import com.neuropace.cognitive.entity.Student;
import com.neuropace.cognitive.entity.Subject;
import com.neuropace.cognitive.model.SlidingWindow;
import com.neuropace.cognitive.repository.LearningEventRepository;
import com.neuropace.cognitive.repository.OverloadDetectionRepository;
import com.neuropace.cognitive.repository.RerouteDecisionRepository;
import com.neuropace.cognitive.repository.StudentRepository;
import com.neuropace.cognitive.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CognitiveLoadService.
 */
@ExtendWith(MockitoExtension.class)
class CognitiveLoadServiceTest {

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private RerouteDecisionRepository rerouteDecisionRepository;

    @Mock
    private OverloadDetectionRepository overloadDetectionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private CognitiveLoadService cognitiveLoadService;

    private Student testStudent(Long id) {
        return Student.builder().id(id).name("Test Student").enrollmentDate(Instant.now()).build();
    }

    private Subject testSubject(Long id) {
        return Subject.builder().id(id).name("Test Subject").difficultyLevel(5).build();
    }

    @Test
    void testRecordLearningEvent_Normal() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent(studentId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(testSubject(subjectId)));

        // 3-event history to establish baseline
        List<LearningEvent> history = new ArrayList<>();
        history.add(LearningEvent.builder().responseTimeMs(1000L).timestamp(Instant.now()).build());
        history.add(LearningEvent.builder().responseTimeMs(1100L).timestamp(Instant.now()).build());
        history.add(LearningEvent.builder().responseTimeMs(1050L).timestamp(Instant.now()).build());

        when(learningEventRepository.findTop20ByStudentIdOrderByTimestampDesc(studentId)).thenReturn(history);
        when(learningEventRepository.save(any(LearningEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(overloadDetectionRepository.save(any(OverloadDetection.class))).thenAnswer(i -> i.getArgument(0));

        // Act — response time of 1080ms is within normal range
        OverloadDetection result = cognitiveLoadService.recordLearningEvent(studentId, subjectId, 1080.0, 4);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(studentId);
        assertThat(result.getIsOverloaded()).isFalse();

        verify(learningEventRepository).save(any(LearningEvent.class));
        verify(overloadDetectionRepository).save(any(OverloadDetection.class));
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void testRecordLearningEvent_Overload() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent(studentId)));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(testSubject(subjectId)));

        List<LearningEvent> history = new ArrayList<>();
        history.add(LearningEvent.builder().responseTimeMs(1000L).timestamp(Instant.now()).build());
        history.add(LearningEvent.builder().responseTimeMs(1100L).timestamp(Instant.now()).build());
        history.add(LearningEvent.builder().responseTimeMs(1050L).timestamp(Instant.now()).build());

        when(learningEventRepository.findTop20ByStudentIdOrderByTimestampDesc(studentId)).thenReturn(history);
        when(learningEventRepository.save(any(LearningEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(overloadDetectionRepository.save(any(OverloadDetection.class))).thenAnswer(i -> i.getArgument(0));

        // Act — 3000ms is a huge spike → overloaded
        OverloadDetection result = cognitiveLoadService.recordLearningEvent(studentId, subjectId, 3000.0, 2);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsOverloaded()).isTrue();

        verify(stringRedisTemplate).convertAndSend(eq("StudentOverloaded"), anyString());
    }

    @Test
    void testGetStudentCognitiveLoad_FromCache() {
        // Arrange
        Long studentId = 10L;
        CognitiveLoadService.CachedWindow cached = new CognitiveLoadService.CachedWindow(new SlidingWindow());
        cached.latestZScore = 3.0;
        cognitiveLoadService.getCache().put(studentId, cached);

        // Act
        double load = cognitiveLoadService.getStudentCognitiveLoad(studentId);

        // Assert: min(1.0, max(0, (3.0 - 1.5) / 3.0)) = 0.5
        assertThat(load).isEqualTo(0.5);
    }

    @Test
    void testGetStudentCognitiveLoad_DatabaseFallback() {
        // Arrange — no cached entry for studentId 20
        Long studentId = 20L;
        OverloadDetection dbRecord = OverloadDetection.builder()
                .studentId(studentId)
                .zScore(4.5)
                .eventTime(Instant.now())
                .responseTimeMs(3000.0)
                .baselineMeanMs(1000.0)
                .baselineStddev(50.0)
                .isOverloaded(true)
                .build();

        when(overloadDetectionRepository.findFirstByStudentIdOrderByEventTimeDesc(studentId))
                .thenReturn(Optional.of(dbRecord));

        // Act
        double load = cognitiveLoadService.getStudentCognitiveLoad(studentId);

        // Assert: min(1.0, max(0, (4.5 - 1.5) / 3.0)) = 1.0
        assertThat(load).isEqualTo(1.0);
    }

    @Test
    void testGetStudentCognitiveLoad_NoHistory() {
        // Arrange — no cache, no DB record
        Long studentId = 99L;
        when(overloadDetectionRepository.findFirstByStudentIdOrderByEventTimeDesc(studentId))
                .thenReturn(Optional.empty());

        // Act
        double load = cognitiveLoadService.getStudentCognitiveLoad(studentId);

        // Assert: zScore=0 → max(0, (0 - 1.5) / 3.0) = 0
        assertThat(load).isEqualTo(0.0);
    }
}
