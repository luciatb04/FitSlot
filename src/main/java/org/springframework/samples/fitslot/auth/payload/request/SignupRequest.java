package org.springframework.samples.fitslot.auth.payload.request;

import org.springframework.samples.fitslot.gym.Gym;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
	
	// User
	@NotBlank
	private String username;
	
	@NotBlank
	private String authority;

	@NotBlank
	private String password;
	
	//Both
	@NotBlank
	private String firstName;
	
	@NotBlank
	private String lastName;
	
	@NotBlank
	private String city;
	private String email;
	private String telephone;
	private Gym gym;

}
