package org.springframework.samples.fitslot.instructor;

import org.springframework.data.repository.CrudRepository;

public interface SpecialtyRepository extends CrudRepository<Specialty, Integer>{
	
//	@Modifying
//	@Query("DELETE i FROM instructor_specialties i WHERE i.specialty_id = :id")
//	void deleteSpecialtyFromInstructors(int id);
	
//	@Query("SELECT v FROM Instructor v JOIN Specialty s WHERE s.id = :id")
//	List<Instructor> findInstructorsBySpecialty(int id);

}
