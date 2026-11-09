package org.springframework.samples.fitslot.instructor;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.samples.fitslot.util.RestPreconditions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Specialties", description = "API for the  management of Specialties")
@RequestMapping("/api/v1/instructors/specialties")
public class SpecialtyRestController {

	private final InstructorService instructorService;

	@Autowired
	public SpecialtyRestController(InstructorService gymService) {
		this.instructorService = gymService;
	}

	@GetMapping
	public List<Specialty> findAll() {
		return (List<Specialty>) instructorService.findSpecialties();
	}

	@GetMapping(value = "{specialtyId}")
	public ResponseEntity<Specialty> findById(@PathVariable("specialtyId") int id) {
		return new ResponseEntity<>(instructorService.findSpecialtyById(id), HttpStatus.OK);
	}

	@PostMapping()
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Specialty> create(@RequestBody Specialty specialty) {
		Specialty newSpecialty = new Specialty();
		BeanUtils.copyProperties(specialty, newSpecialty, "id");
		Specialty savedSpecialty = this.instructorService.saveSpecialty(newSpecialty);

		return new ResponseEntity<>(savedSpecialty, HttpStatus.CREATED);
	}

	@PutMapping(value = "{specialtyId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Specialty> update(@PathVariable("specialtyId") int specialtyId,
			@RequestBody @Valid Specialty specialty) {
		RestPreconditions.checkNotNull(instructorService.findSpecialtyById(specialtyId), "Specialty", "ID", specialtyId);
		return new ResponseEntity<>(this.instructorService.updateSpecialty(specialty, specialtyId), HttpStatus.OK);
	}

	@DeleteMapping(value = "{specialtyId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("specialtyId") int id) {
		RestPreconditions.checkNotNull(instructorService.findSpecialtyById(id), "Specialty", "ID", id);
		instructorService.deleteSpecialty(id);
		return new ResponseEntity<>(new MessageResponse("Specialty deleted!"), HttpStatus.OK);
	}

}
