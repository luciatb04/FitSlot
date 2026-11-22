package org.springframework.samples.fitslot.instructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface InstructorRepository extends CrudRepository<Instructor, Integer> {

	@Query("SELECT DISTINCT instructor FROM Instructor instructor WHERE instructor.user.id = :userId")
	public Optional<Instructor> findInstructorByUser(int userId);

	// STATS
	// ADMIN
	@Query("SELECT COUNT(v) FROM Instructor v")
	public Integer countAll();

	@Query("SELECT NEW MAP(v.city as city, cast(COUNT(v) as string) as instructors)" + " FROM Instructor v GROUP BY v.city")
	public List<Map<String, String>> countInstructorsGroupedByCity();

}
