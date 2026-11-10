package org.springframework.samples.fitslot.gym;

import org.springframework.samples.fitslot.gymowner.GymOwner;
import org.springframework.samples.fitslot.model.BaseEntity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "gyms")
public class Gym extends BaseEntity{

    @Column(name = "name")
	@NotBlank
	private String name;

    @Column(name = "address")
	@NotBlank
	private String address;

    @Column(name = "telephone")
	@NotEmpty
	@Digits(fraction = 0, integer = 10)
	private String telephone;

    @Enumerated(EnumType.STRING)
	@NotNull
	private PricingPlan plan;

    @ManyToOne
	@JoinColumn(name = "gym_owner", referencedColumnName = "id")
	private GymOwner gymOwner;

}
