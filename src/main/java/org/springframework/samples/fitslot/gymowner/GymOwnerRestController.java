package org.springframework.samples.fitslot.gymowner;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;
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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gymOwners")
@SecurityRequirement(name = "bearerAuth")
public class GymOwnerRestController {
    private final GymOwnerService gymOwnerService;
	private final UserService userService;

	@Autowired
	public GymOwnerRestController(GymOwnerService gymOwnerService, UserService userService) {
		this.gymOwnerService = gymOwnerService;
		this.userService = userService;
	}

	@GetMapping(value = "/all")
	public ResponseEntity<List<GymOwner>> getAll() {
		return new ResponseEntity<>((List<GymOwner>) gymOwnerService.findAll(), HttpStatus.OK);
	}

	@GetMapping(value = "/gyms")
	public ResponseEntity<GymOwner> findByUserId(@RequestParam(required = true) Integer userId) {
		return new ResponseEntity<>(gymOwnerService.findByUserId(userId), HttpStatus.OK);
	}

	@GetMapping(value = "/{gymOwnerId}")
	public ResponseEntity<GymOwner> getGymOwnerById(@PathVariable("gymOwnerId") int gymOwnerId) {
		return new ResponseEntity<>(gymOwnerService.findById(gymOwnerId), HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<GymOwner> create(@Valid @RequestBody GymOwner gymOwner, @RequestParam(required = false) int userId) {
		
		System.out.println("ID: " + userId);

		User user = userService.findUser(userId);
		gymOwner.setUser(user);
		
		gymOwnerService.saveGymOwner(gymOwner);
		return new ResponseEntity<>(gymOwner, HttpStatus.CREATED);
	}

	@PutMapping(value = "/{gymOwnerId}")
	public ResponseEntity<GymOwner> create(@PathVariable("gymOwnerId") int gymOwnerId, @Valid @RequestBody GymOwner gymOwner) {
		
		GymOwner gymOwnerToUpdate= gymOwnerService.findById(gymOwnerId);
		BeanUtils.copyProperties(gymOwner, gymOwnerToUpdate, "id", "user", "gyms");
	
		return new ResponseEntity<>(gymOwnerService.saveGymOwner(gymOwnerToUpdate), HttpStatus.CREATED);
	}

	@DeleteMapping(value = "/{gymOwnerId}")
	public ResponseEntity<MessageResponse> delete(@PathVariable("gymOwnerId") int gymOwnerId) {
		gymOwnerService.deleteById(gymOwnerId);
		return new ResponseEntity<>(new MessageResponse("Gym Owner deleted successfully!"), HttpStatus.OK);
	}
}