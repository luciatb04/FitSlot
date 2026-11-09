package org.springframework.samples.fitslot.gymowner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GymOwnerRepository extends CrudRepository<GymOwner, Integer> {

    @Query("SELECT gymOwner FROM GymOwner gymOwner WHERE gymOwner.user.id = :userId")
    Optional<GymOwner> findByUserId(int userId);

}
