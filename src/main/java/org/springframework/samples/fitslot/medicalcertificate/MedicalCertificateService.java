package org.springframework.samples.fitslot.medicalcertificate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MedicalCertificateService {

    private final MedicalCertificateRepository certificateRepository;
    private final MemberService memberService;
    private final Clock clock;

    public MedicalCertificateService(
            MedicalCertificateRepository certificateRepository,
            MemberService memberService,
            Clock clock) {
        this.certificateRepository = certificateRepository;
        this.memberService = memberService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MedicalCertificate> findHistory(Integer memberId) {
        memberService.findById(memberId);
        return certificateRepository.findByMember_IdOrderByIssueDateDescIdDesc(memberId);
    }

    @Transactional
    public MedicalCertificate create(Integer memberId, MedicalCertificateRequest request) {
        validateDates(request);
        Member member = memberService.findById(memberId);

        MedicalCertificate certificate = new MedicalCertificate();
        certificate.setMember(member);
        certificate.setIssueDate(request.issueDate());
        certificate.setExpiryDate(request.expiryDate());
        return certificateRepository.save(certificate);
    }

    @Transactional
    public MedicalCertificate update(Integer memberId, Integer certificateId, MedicalCertificateRequest request) {
        validateDates(request);
        memberService.findById(memberId);
        MedicalCertificate certificate = certificateRepository.findById(certificateId)
            .orElseThrow(() -> new ResourceNotFoundException("Medical certificate", "ID", certificateId));
        if (!certificate.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("Medical certificate", "ID", certificateId);
        }

        certificate.setIssueDate(request.issueDate());
        certificate.setExpiryDate(request.expiryDate());
        return certificateRepository.save(certificate);
    }

    @Transactional(readOnly = true)
    public List<MedicalCertificate> findAllByLatestIssuedFirst(Integer memberId) {
        return findHistory(memberId);
    }

    @Transactional(readOnly = true)
    public MedicalCertificate findLatest(Integer memberId) {
        memberService.findById(memberId);
        return certificateRepository.findFirstByMember_IdOrderByIssueDateDescIdDesc(memberId)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasValidLatestCertificate(Integer memberId) {
        MedicalCertificate latest = findLatest(memberId);
        return latest != null && isValid(latest);
    }

    @Transactional(readOnly = true)
    public boolean isValid(MedicalCertificate certificate) {
        LocalDate today = LocalDate.now(clock);
        return !certificate.getIssueDate().isAfter(today)
            && !certificate.getExpiryDate().isBefore(today);
    }

    @Transactional(readOnly = true)
    public CertificateValidity getValidity(MedicalCertificate certificate) {
        LocalDate today = LocalDate.now(clock);
        if (certificate.getIssueDate().isAfter(today)) {
            return CertificateValidity.NOT_YET_VALID;
        }
        return certificate.getExpiryDate().isBefore(today)
            ? CertificateValidity.EXPIRED
            : CertificateValidity.VALID;
    }

    private void validateDates(MedicalCertificateRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.issueDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issue date cannot be in the future");
        }
        if (request.expiryDate().isBefore(request.issueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be on or after issue date");
        }
    }
}
