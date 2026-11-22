package org.springframework.samples.fitslot.instructor;

import org.springframework.samples.fitslot.gym.Gym;
import org.springframework.samples.fitslot.model.Person;
import org.springframework.samples.fitslot.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "instructors")
@Getter
@Setter
public class Instructor extends Person {

	@OneToOne(cascade = { CascadeType.DETACH, CascadeType.REFRESH, CascadeType.PERSIST })
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private User user;

	@Column(name = "city")
	@NotEmpty
	private String city;

	@ManyToOne
	@JoinColumn(name = "gym", referencedColumnName = "id")
	private Gym gym;

}
