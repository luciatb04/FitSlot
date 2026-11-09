package org.springframework.samples.fitslot.auth;

import java.util.ArrayList;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.fitslot.auth.payload.request.SignupRequest;
import org.springframework.samples.fitslot.gym.GymService;
import org.springframework.samples.fitslot.gymowner.GymOwner;
import org.springframework.samples.fitslot.gymowner.GymOwnerService;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.instructor.InstructorService;
import org.springframework.samples.fitslot.instructor.Specialty;
import org.springframework.samples.fitslot.owner.Owner;
import org.springframework.samples.fitslot.owner.OwnerService;
import org.springframework.samples.fitslot.user.Authorities;
import org.springframework.samples.fitslot.user.AuthoritiesService;
import org.springframework.samples.fitslot.user.User;
import org.springframework.samples.fitslot.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final PasswordEncoder encoder;
	private final AuthoritiesService authoritiesService;
	private final UserService userService;
	private final OwnerService ownerService;
	private final InstructorService instructorService;
	private final GymOwnerService gymOwnerService;
	private final GymService gymService;

	@Autowired
	public AuthService(PasswordEncoder encoder, AuthoritiesService authoritiesService, UserService userService,
			OwnerService ownerService, InstructorService instructorService, GymOwnerService gymOwnerService, GymService gymService) {
		this.encoder = encoder;
		this.authoritiesService = authoritiesService;
		this.userService = userService;
		this.ownerService = ownerService;
		this.instructorService = instructorService;
		this.gymOwnerService = gymOwnerService;
		this.gymService = gymService;
	}

	@Transactional
	public void createUser(@Valid SignupRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(encoder.encode(request.getPassword()));
		String strRoles = request.getAuthority();
		Authorities role;

		switch (strRoles.toLowerCase()) {
		case "admin":
			role = authoritiesService.findByAuthority("ADMIN");
			user.setAuthority(role);
			userService.saveUser(user);
			break;
		case "instructor":
			role = authoritiesService.findByAuthority("INSTRUCTOR");
			user.setAuthority(role);
			userService.saveUser(user);
			Instructor instructor = new Instructor();
			instructor.setFirstName(request.getFirstName());
			instructor.setLastName(request.getLastName());
			instructor.setCity(request.getCity());
			instructor.setSpecialties(new ArrayList<Specialty>());
			instructor.setGym(gymService.findGymById(request.getGym().getId()));
			instructor.setUser(user);
			instructorService.saveInstructor(instructor);
			break;
		case "gym owner":
			role = authoritiesService.findByAuthority("GYM_OWNER");
			user.setAuthority(role);
			userService.saveUser(user);
			GymOwner gymOwner = new GymOwner();
			gymOwner.setFirstName(request.getFirstName());
			gymOwner.setLastName(request.getLastName());
			gymOwner.setUser(user);
			gymOwnerService.saveGymOwner(gymOwner);
			break;
		default:
			role = authoritiesService.findByAuthority("OWNER");
			user.setAuthority(role);
			userService.saveUser(user);
			Owner owner = new Owner();
			owner.setFirstName(request.getFirstName());
			owner.setLastName(request.getLastName());
			owner.setAddress(request.getAddress());
			owner.setCity(request.getCity());
			owner.setTelephone(request.getTelephone());
			owner.setGym(gymService.findGymById(request.getGym().getId()));
			owner.setUser(user);
			ownerService.saveOwner(owner);

		}
	}

}
