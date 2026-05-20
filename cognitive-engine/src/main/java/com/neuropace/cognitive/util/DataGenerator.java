package com.neuropace.cognitive.util;

import com.neuropace.cognitive.entity.*;
import com.neuropace.cognitive.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataGenerator implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningEventRepository learningEventRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (studentRepository.count() > 0) {
            log.info("Database already initialized. Skipping sample data generation.");
            return;
        }

        log.info("Initializing sample data for NeuroPace...");

        // 1. Create 100 sample students
        List<Student> students = new ArrayList<>();
        Random random = new Random();
        for (int i = 1; i <= 100; i++) {
            Student student = Student.builder()
                    .name("Student " + i)
                    .enrollmentDate(Instant.now().minus(random.nextInt(30) + 1, ChronoUnit.DAYS))
                    .avgResponseTime(1500.0 + random.nextInt(1000))
                    .build();
            students.add(student);
        }
        students = studentRepository.saveAll(students);
        log.info("Generated 100 students.");

        // 2. Create 50 sample subjects with difficulty 1-10
        List<Subject> subjects = new ArrayList<>();
        String[] topics = {"Computer Science", "Mathematics", "Physics", "Chemistry", "Biology", "Neuroscience"};
        for (int i = 1; i <= 50; i++) {
            Subject subject = Subject.builder()
                    .name("Subject " + i)
                    .difficultyLevel(random.nextInt(10) + 1)
                    .topicArea(topics[random.nextInt(topics.length)])
                    .build();
            subjects.add(subject);
        }
        subjects = subjectRepository.saveAll(subjects);
        log.info("Generated 50 subjects.");

        // 3. Create a valid DAG of prerequisites (no cycles)
        // We use the subject ID ordering to prevent cycles: a prerequisite can only exist from a lower ID to a higher ID
        List<Prerequisite> prerequisites = new ArrayList<>();
        for (int i = 0; i < subjects.size(); i++) {
            Subject currentSubject = subjects.get(i);
            // 40% chance for a subject to have a prerequisite if it has previous subjects
            if (i > 0 && random.nextDouble() < 0.4) {
                // Select a random prerequisite from previous subjects (guarantees DAG)
                int prereqIndex = random.nextInt(i);
                Subject requiredSubject = subjects.get(prereqIndex);
                Prerequisite prerequisite = Prerequisite.builder()
                        .subject(currentSubject)
                        .requiresSubject(requiredSubject)
                        .weight(0.5 + random.nextDouble() * 0.5)
                        .build();
                prerequisites.add(prerequisite);
            }
        }
        prerequisiteRepository.saveAll(prerequisites);
        log.info("Generated {} valid prerequisites.", prerequisites.size());

        // 4. Enroll students in a few random subjects
        List<Enrollment> enrollments = new ArrayList<>();
        for (Student student : students) {
            // Enroll each student in 2 to 5 random subjects
            int enrollmentCount = random.nextInt(4) + 2;
            Set<Integer> selectedSubjectIndexes = new HashSet<>();
            while (selectedSubjectIndexes.size() < enrollmentCount) {
                selectedSubjectIndexes.add(random.nextInt(subjects.size()));
            }

            for (int index : selectedSubjectIndexes) {
                Enrollment enrollment = Enrollment.builder()
                        .student(student)
                        .subject(subjects.get(index))
                        .enrolledDate(student.getEnrollmentDate().plus(1, ChronoUnit.HOURS))
                        .build();
                enrollments.add(enrollment);
            }
        }
        enrollments = enrollmentRepository.saveAll(enrollments);
        log.info("Generated student enrollments.");

        // 5. Inserts 1000 sample learning events
        List<LearningEvent> learningEvents = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            // Pick a random enrollment to ensure learning events are for enrolled subjects
            Enrollment randomEnrollment = enrollments.get(random.nextInt(enrollments.size()));
            LearningEvent event = LearningEvent.builder()
                    .student(randomEnrollment.getStudent())
                    .subject(randomEnrollment.getSubject())
                    .responseTimeMs(500L + random.nextInt(3000))
                    .performanceScore(random.nextInt(6)) // 0 to 5
                    .timestamp(Instant.now().minus(random.nextInt(24), ChronoUnit.HOURS))
                    .build();
            learningEvents.add(event);
        }
        learningEventRepository.saveAll(learningEvents);
        log.info("Generated 1000 learning events.");
    }
}
