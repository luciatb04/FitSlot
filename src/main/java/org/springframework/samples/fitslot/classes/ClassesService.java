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
package org.springframework.samples.fitslot.classes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.fitslot.classes.exceptions.DuplicatedClassesNameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassesService {

	private final Integer BASIC_LIMIT = 2;
	private final Integer GOLD_LIMIT = 4;
	private final Integer PLATINUM_LIMIT = 7;

	private ClassesRepository classesRepository;

	@Autowired
	public ClassesService(ClassesRepository classRepository) {
		this.classesRepository = classRepository;
	}

	@Transactional(readOnly = true)
	public List<Classes> findAll() {
		return (List<Classes>) classesRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Classes findClassesById(int id) throws DataAccessException {
		return classesRepository.findById(id).orElseThrow();
	}

	@Transactional(rollbackFor = DuplicatedClassesNameException.class)
	public Classes saveClasses(Classes classes) throws DataAccessException, DuplicatedClassesNameException {
		Classes otherClasses = getClassesWithNameAndIdDifferent(classes);
		if (otherClasses != null && !otherClasses.getId().equals(classes.getId())) {
			throw new DuplicatedClassesNameException();
		} else
			classesRepository.save(classes);

		return classes;
	}

	private Classes getClassesWithNameAndIdDifferent(Classes classes) {
		return null;
	}

	@Transactional
	public Classes updateClasses(Classes classes, int id) {
		Classes toUpdate = findClassesById(id);
		BeanUtils.copyProperties(Classes.class, toUpdate, "id");
		return saveClasses(toUpdate);
	}

	@Transactional
	public void deleteClasses(int id) throws DataAccessException {
		Classes toDelete = findClassesById(id);
		classesRepository.delete(toDelete);
	}

	public Map<String, Object> getClassesStats() {
		Map<String, Object> res = new HashMap<>();
		Integer countAll = this.classesRepository.countAll();

		res.put("totalClasses", countAll);
		return res;
	}
}
