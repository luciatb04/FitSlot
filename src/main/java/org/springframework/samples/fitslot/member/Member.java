package org.springframework.samples.fitslot.member;

import org.springframework.samples.fitslot.membership.MembershipTier;
import org.springframework.samples.fitslot.model.Person;
import org.springframework.samples.fitslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
public class Member extends Person {

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

}