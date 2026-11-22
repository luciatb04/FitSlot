
package org.springframework.samples.fitslot.classes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface ClassReservationRepository
        extends CrudRepository<ClassReservation, Integer> {

    boolean existsByMember_IdAndClassSession_Id(
            Integer memberId,
            Integer sessionId);

    List<ClassReservation>
            findByClassSession_IdOrderByReservationTimeAsc(
                    Integer sessionId);

    long countByClassSession_IdAndStatus(
            Integer sessionId,
            ReservationStatus status);

    Optional<ClassReservation> findByMember_IdAndClassSession_Id(
            Integer memberId,
            Integer sessionId);

    List<ClassReservation>
            findByClassSession_IdAndStatusOrderByReservationTimeAsc(
                    Integer sessionId,
                    ReservationStatus status);
}
