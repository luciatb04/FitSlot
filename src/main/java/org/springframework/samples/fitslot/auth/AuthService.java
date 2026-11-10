package org.springframework.samples.fitslot.auth;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.fitslot.auth.payload.request.SignupRequest;
import org.springframework.samples.fitslot.gym.GymService;
import org.springframework.samples.fitslot.gymowner.GymOwner;
import org.springframework.samples.fitslot.gymowner.GymOwnerService;
import org.springframework.samples.fitslot.instructor.Instructor;
import org.springframework.samples.fitslot.instructor.InstructorService;
import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.member.MemberService;
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
	private final InstructorService instructorService;
	private final GymOwnerService gymOwnerService;
	private final GymService gymService;
	private final MemberService memberService;

	@Autowired
	public AuthService(PasswordEncoder encoder, AuthoritiesService authoritiesService, UserService userService,
			InstructorService instructorService, GymOwnerService gymOwnerService, GymService gymService,
			MemberService memberService) {
		this.encoder = encoder;
		this.authoritiesService = authoritiesService;
		this.userService = userService;
		this.instructorService = instructorService;
		this.gymOwnerService = gymOwnerService;
		this.gymService = gymService;
		this.memberService = memberService;
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
		case "member":
			if (request.getEmail() == null || request.getEmail().isBlank()) {
				throw new IllegalArgumentException("Email is required for members");
			}
			role = authoritiesService.findByAuthority("MEMBER");
			user.setAuthority(role);
			userService.saveUser(user);
			Member member = new Member();
			member.setFirstName(request.getFirstName());
			member.setLastName(request.getLastName());
			member.setEmail(request.getEmail());
			member.setTelephone(request.getTelephone());
			member.setUser(user);
			memberService.saveMember(member);
			break;
		case "receptionist":
			role = authoritiesService.findByAuthority("RECEPTIONIST");
			user.setAuthority(role);
			userService.saveUser(user);
			break;
		default:
			throw new IllegalArgumentException("Unsupported authority: " + strRoles);
		}
	}

}
