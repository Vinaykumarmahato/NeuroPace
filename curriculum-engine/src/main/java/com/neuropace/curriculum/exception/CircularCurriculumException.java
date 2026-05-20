package com.neuropace.curriculum.exception;

import com.neuropace.curriculum.entity.Subject;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CircularCurriculumException extends RuntimeException {
    private final List<Subject> cycle;

    public CircularCurriculumException(List<Subject> cycle) {
        super(formatMessage(cycle));
        this.cycle = List.copyOf(cycle);
    }

    private static String formatMessage(List<Subject> cycle) {
        String cyclePath = cycle.stream()
                .map(Subject::getName)
                .collect(Collectors.joining(" -> "));
        return "Circular curriculum detected: " + cyclePath;
    }
}
