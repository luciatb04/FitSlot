// classes/ClassReservationRepository.java
package org.springframework.samples.fitslot.classes;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ClassReservationRepository
        extends CrudRepository<ClassReservation, Integer> {
    boolean existsByMember_IdAndClassSession_Id(Integer memberId, Integer sessionId);

    List<ClassReservation>
        findByClassSession_IdOrderByReservationTimeAsc(Integer sessionId);
}