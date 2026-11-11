package org.springframework.samples.fitslot.membership.TimeBandConfiguration;

import java.time.LocalTime;

import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.model.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "time_band_configurations")
@Getter
@Setter
public class TimeBandConfiguration extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TimeBand timeBand;

    @NotNull
    @Column(nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(nullable = false)
    private LocalTime endTime;
}
