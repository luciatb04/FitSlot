package org.springframework.samples.fitslot.medicalcertificate;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MedicalCertificateRequest(
    @NotNull LocalDate issueDate,
    @NotNull LocalDate expiryDate
) {
}
