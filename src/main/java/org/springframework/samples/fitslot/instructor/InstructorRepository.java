package org.springframework.samples.fitslot.instructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface InstructorRepository extends CrudRepository<Instructor, Integer> {

	@Query("SELECT DISTINCT instructor FROM Instructor instructor WHERE instructor.user.id = :userId")
	public Optional<Instructor> findInstructorByUser(int userId);

	List<Instructor> findByGym_Id(Integer gymId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Instructor i WHERE i.id = :id")
	Optional<Instructor> findByIdForUpdate(
			@Param("id") Integer id);

	@Query("SELECT COUNT(v) FROM Instructor v")
	public Integer countAll();

	@Query("SELECT NEW MAP(v.city as city, cast(COUNT(v) as string) as instructors)"
			+ " FROM Instructor v GROUP BY v.city")
	public List<Map<String, String>> countInstructorsGroupedByCity();

}
