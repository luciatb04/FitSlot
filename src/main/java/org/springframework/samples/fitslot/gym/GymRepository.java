package org.springframework.samples.fitslot.gym;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.owner.Owner;

public interface GymRepository extends CrudRepository<Gym, Integer> {

    @Query("SELECT o FROM Owner o WHERE o.gym.gymOwner.user.id = :userId")
    List<Owner> findOwnersOfUserGyms(int userId);

    @Query("SELECT v FROM Instructor v WHERE v.gym.gymOwner.user.id = :userId")
    List<Instructor> findInstructorsOfUserGyms(int userId);

    @Query("SELECT c FROM Gym c WHERE c.gymOwner.user.id = :userId")
    List<Gym> findGymsByUserId(int userId);
}
