package org.springframework.samples.fitslot.gym;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.fitslot.instructor.Instructor;

public interface GymRepository extends CrudRepository<Gym, Integer> {

    @Query("SELECT v FROM Instructor v WHERE v.gym.gymOwner.user.id = :userId")
    List<Instructor> findInstructorsOfUserGyms(int userId);

    @Query("SELECT c FROM Gym c WHERE c.gymOwner.user.id = :userId")
    List<Gym> findGymsByUserId(int userId);
}
