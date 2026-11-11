package org.springframework.samples.fitslot.medicalcertificate;

import java.time.LocalDate;

public record MedicalCertificateResponse(
    Integer id,
    Integer memberId,
    LocalDate issueDate,
    LocalDate expiryDate,
    CertificateValidity validity
) {
}
