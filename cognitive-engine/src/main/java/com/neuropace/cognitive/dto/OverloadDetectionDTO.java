package com.neuropace.cognitive.dto;

/**
 * Data Transfer Object representing the result of a cognitive overload detection check.
 */
public record OverloadDetectionDTO(double zScore, boolean isOverloaded, String reason) {
}
