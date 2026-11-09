package org.springframework.samples.fitslot.consultation;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends CrudRepository<Consultation, Integer> {

	@Query("SELECT c FROM Consultation c WHERE c.owner.id = :ownerId ORDER BY c.creationDate DESC")
	public List<Consultation> findConsultationsByOwner(@Param("ownerId") int ownerId);

//	@Query("SELECT c FROM Consultation c JOIN Ticket t WHERE t.consultation.id = c.id AND t.user.id = :userId ORDER BY c.creationDate DESC")
//	public List<Consultation> findConsultationsByUser(@Param("userId") int userId);

	public Iterable<Consultation> findAllByOrderByCreationDateDesc();

//	@Query("SELECT c FROM Consultation c JOIN Instructor v WHERE c.id = v.consul ORDER BY c.creationDate DESC")
//	public List<Consultation> findConsultationsByInstructor(@Param("instructorId") int instructorId);

	// STATS
	// ADMIN
	@Query("SELECT COUNT(c) FROM Consultation c")
	public Integer countAll();

	@Query("SELECT COUNT(o) FROM Owner o WHERE o.gym.plan = 'PLATINUM'")
	public Integer countAllPlatinums();

	@Query("SELECT COUNT(o) FROM Owner o")
	public Integer countAllOwners();

	// OWNER
	@Query("SELECT COUNT(c) FROM Consultation c WHERE c.owner.id = :ownerId")
	public Integer countAllByOwner(int ownerId);

	@Query("SELECT COUNT(p) FROM Classes p WHERE p.owner.id = :ownerId")
	public Integer countAllClassesOfOwner(int ownerId);

	@Query("SELECT NEW MAP(c.classes.name as Class, cast(COUNT(c) as string) as consultations)"
			+ " FROM Consultation c WHERE c.owner.id = :ownerId GROUP BY c.classes")
	public List<Map<String, String>> countConsultationsGroupedByClasses(int ownerId);

	@Query("SELECT MIN(YEAR(c.creationDate)) FROM Consultation c WHERE c.owner.id = :ownerId")
	public Integer getYearOfFirstConsultation(int ownerId);

	@Query("SELECT NEW MAP(YEAR(c.creationDate) as year, cast(COUNT(c) as integer) as consultations)"
			+ " FROM  Consultation c WHERE c.owner.id = :ownerId GROUP BY YEAR(c.creationDate)")
	public List<Map<String, Integer>> countConsultationsGroupedByYear(int ownerId);

	@Query("SELECT c FROM Consultation c WHERE c.owner.gym.gymOwner.user.id = :userId")
	public List<Consultation> findAllByGymOwnerUserId(int userId);

	@Query("SELECT c FROM Consultation c WHERE c.owner.gym.id = :gymId")
	public List<Consultation> findAllByGymId(int gymId);

}
