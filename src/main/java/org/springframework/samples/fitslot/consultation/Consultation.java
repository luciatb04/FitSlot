package org.springframework.samples.fitslot.consultation;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.samples.fitslot.model.BaseEntity;
import org.springframework.samples.fitslot.owner.Owner;
import org.springframework.samples.fitslot.classes.Classes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "consultations")
@Getter
@Setter
public class Consultation extends BaseEntity {

	@Column(name = "title")
	@NotEmpty
	private String title;

	@Column(name = "is_gym_comment")
	@NotNull
	private Boolean isGymComment = false;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private ConsultationStatus status;

	@ManyToOne
	@JoinColumn(name = "owner_id")	
	@NotNull
	private Owner owner;

	@ManyToOne
	@JoinColumn(name = "classes_id")	
	private Classes classes;

//	@OneToOne
//	@JoinColumn(name = "instructor_id", nullable = false)
//	private Instructor instructor;

	@Column(name = "creation_date")
	@CreationTimestamp
	private LocalDateTime creationDate;

}
