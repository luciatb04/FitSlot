package org.springframework.samples.fitslot.access;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
import org.springframework.samples.fitslot.medicalcertificate.CertificateValidity;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificate;
import org.springframework.samples.fitslot.medicalcertificate.MedicalCertificateService;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessService {

    private final AccessRecordRepository accessRecordRepository;
    private final MemberService memberService;
    private final MedicalCertificateService certificateService;
    private final Clock clock;

    public AccessService(
            AccessRecordRepository accessRecordRepository,
            MemberService memberService,
            MedicalCertificateService certificateService,
            Clock clock) {
        this.accessRecordRepository = accessRecordRepository;
        this.memberService = memberService;
        this.certificateService = certificateService;
        this.clock = clock;
    }

    @Transactional
    public CheckInResponse checkIn(Integer memberId, ServiceType serviceAccessed) {
        Member member = memberService.findById(memberId);
        MedicalCertificate latest = certificateService.findLatest(memberId);
        if (latest == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: no medical certificate registered");
        }
        if (!certificateService.isValid(latest)) {
            String reason = certificateService.getValidity(latest) == CertificateValidity.EXPIRED
                ? "Access denied: latest medical certificate has expired"
                : "Access denied: latest medical certificate is not valid yet";
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
        }

        LocalDateTime accessDatetime = LocalDateTime.now(clock);
        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setMember(member);
        accessRecord.setAccessDatetime(accessDatetime);
        accessRecord.setServiceAccessed(serviceAccessed);
        AccessRecord saved = accessRecordRepository.save(accessRecord);
        return new CheckInResponse(saved.getId(), memberId, accessDatetime, serviceAccessed.name(), "ALLOWED");
    }
}
