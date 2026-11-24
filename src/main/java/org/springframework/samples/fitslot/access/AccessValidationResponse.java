package org.springframework.samples.fitslot.access;

public record AccessValidationResponse(
    boolean authorized,
    String reason
) {
}
