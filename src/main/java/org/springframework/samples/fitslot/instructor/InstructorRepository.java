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

	@Query("SELECT NEW MAP(v.instructor.firstName as firstName, v.instructor.lastName as lastName, cast(COUNT(v) as string) as visits)"
			+ " FROM Visit v GROUP BY v.instructor")
	public List<Map<String, String>> countVisitsGroupedByInstructor();

}
