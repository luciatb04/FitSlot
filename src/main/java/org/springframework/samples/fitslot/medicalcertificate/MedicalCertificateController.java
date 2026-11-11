package org.springframework.samples.fitslot.medicalcertificate;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/members/{memberId}/medical-certificates")
public class MedicalCertificateController {

    private final MedicalCertificateService certificateService;

    public MedicalCertificateController(MedicalCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public List<MedicalCertificateResponse> findHistory(@PathVariable Integer memberId) {
        return certificateService.findAllByLatestIssuedFirst(memberId).stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping
    public ResponseEntity<MedicalCertificateResponse> create(
            @PathVariable Integer memberId,
            @Valid @RequestBody MedicalCertificateRequest request) {
        MedicalCertificate created = certificateService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{certificateId}")
    public MedicalCertificateResponse update(
            @PathVariable Integer memberId,
            @PathVariable Integer certificateId,
            @Valid @RequestBody MedicalCertificateRequest request) {
        return toResponse(certificateService.update(memberId, certificateId, request));
    }

    private MedicalCertificateResponse toResponse(MedicalCertificate certificate) {
        return new MedicalCertificateResponse(
            certificate.getId(),
            certificate.getMember().getId(),
            certificate.getIssueDate(),
            certificate.getExpiryDate(),
            certificateService.getValidity(certificate)
        );
    }
}
