// access/AccessRecordRepository.java
package org.springframework.samples.fitslot.access;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface AccessRecordRepository extends CrudRepository<AccessRecord, Integer> {
    List<AccessRecord> findByMember_IdOrderByAccessDatetimeDesc(Integer memberId);
}