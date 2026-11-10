package org.springframework.samples.fitslot.membership;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.samples.fitslot.model.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "membership_tiers")
@Getter
@Setter
public class MembershipTier extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private Integer durationDays;

    private Integer entryQuota;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tier_services",
        joinColumns = @JoinColumn(name = "tier_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private Set<ServiceType> allowedServices = new HashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeBand timeBand;
}