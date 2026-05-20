package com.neuropace.cognitive.service;

import com.neuropace.cognitive.entity.Student;
import com.neuropace.cognitive.exception.EntityNotFoundException;
import com.neuropace.cognitive.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return Optional.ofNullable(id)
                .flatMap(studentRepository::findById);
    }

    @Transactional
    public Student createStudent(Student student) {
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudentAvgResponseTime(Long id, Double avgResponseTime) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student", id));
        student.setAvgResponseTime(avgResponseTime);
        return studentRepository.save(student);
    }
}
