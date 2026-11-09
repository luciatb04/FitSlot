/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.fitslot.instructor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorService {

	private InstructorRepository instructorRepository;
	private SpecialtyRepository specialtyRepository;

	@Autowired
	public InstructorService(InstructorRepository instructorRepository, SpecialtyRepository specialtyRepository) {
		this.instructorRepository = instructorRepository;
		this.specialtyRepository = specialtyRepository;
	}

	@Transactional(readOnly = true)
	public Iterable<Instructor> findAll() throws DataAccessException {
		return instructorRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Instructor findInstructorById(int id) throws DataAccessException {
		return instructorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Instructor", "ID", id));
	}

	@Transactional(readOnly = true)
	public Instructor findInstructorByUser(int userId) throws DataAccessException {
		return instructorRepository.findInstructorByUser(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Instructor", "User", userId));
	}

	public Optional<Instructor> optFindInstructorByUser(Integer userId) {
		return instructorRepository.findInstructorByUser(userId);
	}

	@Transactional
	public Instructor saveInstructor(Instructor instructor) throws DataAccessException {
		instructorRepository.save(instructor);
		return instructor;
	}

	@Transactional
	public Instructor updateInstructor(Instructor instructor, int id) throws DataAccessException {
		Instructor toUpdate = findInstructorById(id);
		BeanUtils.copyProperties(instructor, toUpdate, "id", "user");
		instructorRepository.save(toUpdate);

		return toUpdate;
	}

	@Transactional
	public void deleteInstructor(int id) throws DataAccessException {
		Instructor toDelete = findInstructorById(id);
		toDelete.removeAllSpecialties();
		instructorRepository.delete(toDelete);
	}

	@Transactional(readOnly = true)
	public Iterable<Specialty> findSpecialties() throws DataAccessException {
		return specialtyRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Specialty findSpecialtyById(int id) throws DataAccessException {
		return specialtyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Specialty", "ID", id));
	}

	@Transactional
	public Specialty saveSpecialty(Specialty specialty) throws DataAccessException {
		specialtyRepository.save(specialty);

		return specialty;
	}

	@Transactional
	public Specialty updateSpecialty(Specialty specialty, int id) throws DataAccessException {
		Specialty toUpdate = findSpecialtyById(id);
		BeanUtils.copyProperties(specialty, toUpdate, "id");
		specialtyRepository.save(toUpdate);

		return toUpdate;
	}

	@Transactional
	public void deleteSpecialty(int id) throws DataAccessException {
		Specialty toDelete = findSpecialtyById(id);
		for (Instructor v : findAll())
			v.removeSpecialty(toDelete);
		specialtyRepository.delete(toDelete);
	}

//	@Transactional
//	public Instructor addSpecialty(Instructor instructor, Specialty specialty) {
//		List<Specialty> specialties = instructor.getSpecialties();
//		if(!specialties.contains(specialty)) {
//			specialties.add(specialty);
//			instructor.setSpecialties(specialties);
//			instructorRepository.save(instructor);
//		}
//		return instructor;
//	}
//
//	public Instructor removeSpecialty(Instructor instructor, Specialty specialty) {
////		List<Specialty> specialties = instructor.getSpecialties();
////		if(specialties.contains(specialty)) {
////			specialties.remove(specialty);
////			instructor.setSpecialties(specialties);
////			instructorRepository.save(instructor);
////		}
//		instructor.removeSpecialty(specialty);
//		return instructor;
//	}

	public Map<String, Object> getInstructorsStats() {
		Map<String, Object> res = new HashMap<>();
		Integer countAll = this.instructorRepository.countAll();
		Map<String, Integer> visitsByInstructor = getVisitsByInstructor();
		Map<String, Integer> instructorsByCity = getInstructorsByCity();
		Map<String, Integer> instructorsBySpecialty = getInstructorsBySpecialty();

		res.put("totalInstructors", countAll);
		res.put("visitsByInstructor", visitsByInstructor);
		res.put("instructorsByCity", instructorsByCity);
		res.put("instructorsBySpecialty", instructorsBySpecialty);

		return res;
	}

	private Map<String, Integer> getInstructorsBySpecialty() {
		Map<String, Integer> res = new HashMap<>();
		List<Instructor> instructors = (List<Instructor>) findAll();
		for (Instructor v : instructors) {
			if (!v.getSpecialties().isEmpty()) {
				for (Specialty s : v.getSpecialties()) {
					if (res.containsKey(s.getName())) {
						res.put(s.getName(), res.get(s.getName()) + 1);
					} else {
						res.put(s.getName(), 1);
					}
				}
			}
		}
		return res;
	}

	private Map<String, Integer> getVisitsByInstructor() {
		Map<String, Integer> unsortedVisitsByInstructor = new HashMap<>();
		this.instructorRepository.countVisitsGroupedByInstructor().forEach(m -> {
			String key = m.get("firstName") + " " + m.get("lastName");
			Integer value = Integer.parseInt(m.get("visits"));
			unsortedVisitsByInstructor.put(key, value);
		});
		return unsortedVisitsByInstructor.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
	}

	private Map<String, Integer> getInstructorsByCity() {
		Map<String, Integer> unsortedInstructorsByCity = new HashMap<>();
		this.instructorRepository.countInstructorsGroupedByCity().forEach(m -> {
			String key = m.get("city");
			Integer value = Integer.parseInt(m.get("instructors"));
			unsortedInstructorsByCity.put(key, value);
		});
		return unsortedInstructorsByCity;
	}

}
