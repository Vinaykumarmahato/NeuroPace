package com.neuropace.scheduler.service;

import com.neuropace.scheduler.entity.DueCard;
import com.neuropace.scheduler.repository.CardScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Service-level unit tests for SM2SchedulerService.
 */
@ExtendWith(MockitoExtension.class)
class SM2SchedulerServiceTest {

    @Mock
    private CardScheduleRepository cardScheduleRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Spy
    private SM2Scheduler sm2Scheduler = new SM2Scheduler();

    @InjectMocks
    private SM2SchedulerService schedulerService;

    @Test
    void testRecordStudentPerformance_NewCard() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 5L;
        int score = 4;

        when(cardScheduleRepository.findByStudentIdAndSubjectId(studentId, subjectId))
                .thenReturn(Optional.empty());
        when(cardScheduleRepository.save(any(DueCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DueCard result = schedulerService.recordStudentPerformance(studentId, subjectId, score);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(studentId);
        assertThat(result.getSubjectId()).isEqualTo(subjectId);
        assertThat(result.getIntervalDays()).isEqualTo(1.0);
        assertThat(result.getEaseFactor()).isEqualTo(1.5);
        assertThat(result.getRepetitionCount()).isEqualTo(1);
        assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(1));

        verify(cardScheduleRepository).save(any(DueCard.class));
        verify(stringRedisTemplate).convertAndSend(eq("CardScheduled"), anyString());
    }

    @Test
    void testRecordStudentPerformance_ExistingCard() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 5L;
        int score = 5;

        DueCard existingCard = DueCard.builder()
                .studentId(studentId)
                .subjectId(subjectId)
                .easeFactor(2.5)
                .intervalDays(1.0)
                .repetitionCount(1)
                .dueDate(LocalDate.now())
                .build();

        when(cardScheduleRepository.findByStudentIdAndSubjectId(studentId, subjectId))
                .thenReturn(Optional.of(existingCard));
        when(cardScheduleRepository.save(any(DueCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DueCard result = schedulerService.recordStudentPerformance(studentId, subjectId, score);

        // Assert
        assertThat(result.getIntervalDays()).isEqualTo(2.0);
        assertThat(result.getEaseFactor()).isEqualTo(2.5);
        assertThat(result.getRepetitionCount()).isEqualTo(2);

        verify(cardScheduleRepository).save(existingCard);
        verify(stringRedisTemplate).convertAndSend(eq("CardScheduled"), anyString());
    }

    @Test
    void testGetDueCardsForStudent() {
        // Arrange
        Long studentId = 1L;
        DueCard card1 = DueCard.builder().studentId(studentId).dueDate(LocalDate.now().plusDays(5)).build();
        DueCard card2 = DueCard.builder().studentId(studentId).dueDate(LocalDate.now().plusDays(2)).build();

        when(cardScheduleRepository.findByStudentId(studentId))
                .thenReturn(Arrays.asList(card1, card2));

        // Act
        List<DueCard> result = schedulerService.getDueCardsForStudent(studentId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.now().plusDays(2)); // Sorted by comparable dueDate
        assertThat(result.get(1).getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
    }
}
