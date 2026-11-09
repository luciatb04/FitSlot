package org.springframework.samples.fitslot.instructor;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;
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
@RequestMapping("/api/v1/instructors")
@Tag(name = "Instructors", description = "API for the  management of Instructors")
public class InstructorRestController {

	private final InstructorService instructorService;
	private final UserService userService;

	@Autowired
	public InstructorRestController(InstructorService gymService, UserService userService) {
		this.instructorService = gymService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<Instructor>> findAll() {
		List<Instructor> res = (List<Instructor>) this.instructorService.findAll();
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@GetMapping(value = "{instructorId}")
	public ResponseEntity<Instructor> findById(@PathVariable("instructorId") int id) {
		return new ResponseEntity<>(instructorService.findInstructorById(id), HttpStatus.OK);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Instructor> create(@RequestBody @Valid Instructor instructor) throws URISyntaxException {
		Instructor newInstructor = new Instructor();
		BeanUtils.copyProperties(instructor, newInstructor, "id");
		User user = userService.findCurrentUser();
		newInstructor.setUser(user);
		Instructor savedInstructor = this.instructorService.saveInstructor(newInstructor);

		return new ResponseEntity<>(savedInstructor, HttpStatus.CREATED);
	}

	@PutMapping(value = "{instructorId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Instructor> update(@PathVariable("instructorId") int instructorId, @RequestBody @Valid Instructor instructor) {
		RestPreconditions.checkNotNull(instructorService.findInstructorById(instructorId), "Instructor", "ID", instructorId);
		return new ResponseEntity<>(this.instructorService.updateInstructor(instructor, instructorId), HttpStatus.OK);
	}

	@DeleteMapping(value = "{instructorId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("instructorId") int id) {
		RestPreconditions.checkNotNull(instructorService.findInstructorById(id), "Instructor", "ID", id);
		instructorService.deleteInstructor(id);
		return new ResponseEntity<>(new MessageResponse("Instructor deleted!"), HttpStatus.OK);
	}

	@GetMapping(value = "stats")
	public ResponseEntity<Map<String, Object>> getStats() {
		return new ResponseEntity<>(this.instructorService.getInstructorsStats(), HttpStatus.OK);
	}

}
