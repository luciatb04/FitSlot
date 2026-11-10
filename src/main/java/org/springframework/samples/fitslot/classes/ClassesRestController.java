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

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.samples.fitslot.exceptions.AccessDeniedException;
import org.springframework.samples.fitslot.classes.exceptions.DuplicatedClassesNameException;
import org.springframework.samples.fitslot.util.RestPreconditions;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/classes")
@Tag(name = "Classes", description = "The Classes management API")
@SecurityRequirement(name = "bearerAuth")
public class ClassesRestController {

	private final ClassesService classesService;
	@Autowired
	public ClassesRestController(ClassesService classesService) {
		this.classesService = classesService;
	}
/**
 * 
 * @param dataBinder
*/
	@GetMapping
	public ResponseEntity<List<Classes>> findAll() {
		return new ResponseEntity<>(this.classesService.findAll(), HttpStatus.OK);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Classes> create(@RequestBody @Valid Classes classEntity)
			throws DataAccessException, DuplicatedClassesNameException {
		Classes newClasses = new Classes();
		Classes savedClasses;
		BeanUtils.copyProperties(classEntity, newClasses, "id");
		savedClasses = this.classesService.saveClasses(newClasses);

		return new ResponseEntity<>(savedClasses, HttpStatus.CREATED);
	}

	@PutMapping("{classId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Classes> update(@PathVariable("classId") int classId, @RequestBody @Valid Classes classEntity) {
		Classes aux = RestPreconditions.checkNotNull(classesService.findClassesById(classId), "class", "ID", classId);
		Classes res = this.classesService.updateClasses(classEntity, classId);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping("{classesId}")
	public ResponseEntity<Classes> findById(@PathVariable("classesId") int classesId) {
		Classes classEntity = RestPreconditions.checkNotNull(classesService.findClassesById(classesId), "class", "ID", classesId);
		return new ResponseEntity<>(this.classesService.findClassesById(classesId), HttpStatus.OK);
	}

	@DeleteMapping("{classesId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("classesId") int classId) {
		Classes classEntity = RestPreconditions.checkNotNull(classesService.findClassesById(classId), "class", "ID", classId);
		classesService.deleteClasses(classId);
		return new ResponseEntity<>(new MessageResponse("class deleted!"), HttpStatus.OK);
	}

	@GetMapping(value = "stats")
	public ResponseEntity<Map<String, Object>> getStats() {
		return new ResponseEntity<>(this.classesService.getClassesStats(), HttpStatus.OK);
	}

}
