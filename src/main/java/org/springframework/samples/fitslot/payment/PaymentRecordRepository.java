// payment/PaymentRecordRepository.java
package org.springframework.samples.fitslot.payment;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRecordRepository extends CrudRepository<PaymentRecord, Integer> {
    List<PaymentRecord> findByMember_IdOrderByPaymentDateDesc(Integer memberId);
}