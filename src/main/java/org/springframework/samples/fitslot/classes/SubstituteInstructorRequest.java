package org.springframework.samples.fitslot.classes;

import jakarta.validation.constraints.NotNull;

public record SubstituteInstructorRequest(
    @NotNull Integer instructorId
) {}
