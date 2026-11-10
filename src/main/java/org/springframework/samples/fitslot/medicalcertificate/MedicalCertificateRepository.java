// medicalcertificate/MedicalCertificateRepository.java
package org.springframework.samples.fitslot.medicalcertificate;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface MedicalCertificateRepository
        extends CrudRepository<MedicalCertificate, Integer> {
    List<MedicalCertificate> findByMember_IdOrderByExpiryDateDesc(Integer memberId);
}