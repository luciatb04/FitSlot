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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorService {

	private InstructorRepository instructorRepository;

	@Autowired
	public InstructorService(InstructorRepository instructorRepository) {
		this.instructorRepository = instructorRepository;
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
		instructorRepository.delete(toDelete);
	}

	public Map<String, Object> getInstructorsStats() {
		Map<String, Object> res = new HashMap<>();
		Integer countAll = this.instructorRepository.countAll();
		Map<String, Integer> visitsByInstructor = getVisitsByInstructor();
		Map<String, Integer> instructorsByCity = getInstructorsByCity();

		res.put("totalInstructors", countAll);
		res.put("visitsByInstructor", visitsByInstructor);
		res.put("instructorsByCity", instructorsByCity);

		return res;
	}

	private Map<String, Integer> getVisitsByInstructor() {
		return new LinkedHashMap<>();
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
