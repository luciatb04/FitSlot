package org.springframework.samples.fitslot.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.fitslot.instructor.Instructor;

public interface UserRepository extends  CrudRepository<User, Integer>{
	@Query("SELECT v FROM Instructor v WHERE v.user.id = :userId")
	Optional<Instructor> findInstructorByUser(int userId);

	Optional<User> findByUsername(String username);

	Boolean existsByUsername(String username);

	Optional<User> findById(Integer id);
	
	@Query("SELECT u FROM User u WHERE u.authority.authority = :auth")
	Iterable<User> findAllByAuthority(String auth);
	
	@Query("DELETE FROM Instructor v WHERE v.user.id = :userId")
	@Modifying
	void deleteInstructorRelation(int userId);
	
}
