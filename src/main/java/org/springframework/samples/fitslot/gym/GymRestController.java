package org.springframework.samples.fitslot.gym;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.samples.fitslot.gymowner.GymOwner;
import org.springframework.samples.fitslot.gymowner.GymOwnerService;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.user.UserService;
import org.springframework.samples.fitslot.util.RestPreconditions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gyms")
@Tag(name = "Gyms", description = "The Gyms managemet API")
@SecurityRequirement(name = "bearerAuth")
public class GymRestController {
	private final GymService gymService;
	private final GymOwnerService gymOwnerService;
	private final UserService userService;

	@Autowired
	public GymRestController(GymService gymService, GymOwnerService gymOwnerService,
			UserService userService) {
		this.gymService = gymService;
		this.gymOwnerService = gymOwnerService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<Gym>> findAllGyms(@RequestParam(required = false) Integer userId) {
		
		if (userId != null) {
			return new ResponseEntity<>(gymService.findGymsByUserId(userId), HttpStatus.OK);
		}

		return new ResponseEntity<>(gymService.findAll(), HttpStatus.OK);
	}

	@GetMapping(value = "{gymId}")
	public ResponseEntity<Gym> findGymById(@PathVariable("gymId") int gymId) {
		return new ResponseEntity<>(gymService.findGymById(gymId), HttpStatus.OK);
	}

	@GetMapping(value = "instructors")
	public ResponseEntity<List<Instructor>> findInstructorsOfUserGyms(@RequestParam int userId) {
		return new ResponseEntity<>(gymService.findInstructorsOfUserGyms(userId), HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<Gym> createGym(@RequestBody @Valid Gym gym) {

		Gym newGym = new Gym();
		BeanUtils.copyProperties(gym, newGym, "id");
		if(gym.getGymOwner() == null){
			GymOwner owner = gymOwnerService.findByUserId(userService.findCurrentUser().getId());
			newGym.setGymOwner(owner);
		}


		return new ResponseEntity<>(gymService.save(newGym), HttpStatus.CREATED);
	}

	@PutMapping(value = "{gymId}")
	public ResponseEntity<Gym> updateGym(@PathVariable("gymId") int gymId,
			@RequestBody @Valid Gym gym) {
		RestPreconditions.checkNotNull(gymService.findGymById(gymId), "Gym", "ID", gymId);

		return new ResponseEntity<>(gymService.update(gym, gymId), HttpStatus.OK);
	}

	@DeleteMapping(value = "{gymId}")
	public ResponseEntity<MessageResponse> deleteGym(@PathVariable("gymId") int gymId) {
		RestPreconditions.checkNotNull(gymService.findGymById(gymId), "Gym", "ID", gymId);
		gymService.delete(gymId);
		return new ResponseEntity<>(new MessageResponse("Gym deleted!"), HttpStatus.OK);
	}
}