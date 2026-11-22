package org.springframework.samples.fitslot.classes.InstructorSubstitution;

import jakarta.validation.constraints.NotNull;

public record SubstituteInstructorRequest(
    @NotNull Integer instructorId
) {}
