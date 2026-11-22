package org.springframework.samples.fitslot.gymowner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface GymOwnerRepository extends CrudRepository<GymOwner, Integer> {

    @Query("SELECT gymOwner FROM GymOwner gymOwner WHERE gymOwner.user.id = :userId")
    Optional<GymOwner> findByUserId(int userId);

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM GymOwner o
            JOIN o.gyms g
            WHERE o.user.id = :userId
            AND g.id = :gymId
            """)
    boolean managesGym(
            @Param("userId") Integer userId,
            @Param("gymId") Integer gymId);

}
