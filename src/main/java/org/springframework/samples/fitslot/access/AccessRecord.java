package org.springframework.samples.fitslot.access;

import java.time.LocalDateTime;

import org.springframework.samples.fitslot.member.Member;
import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "access_records")
@Getter
@Setter
public class AccessRecord extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @Column(name = "access_datetime", nullable = false)
    private LocalDateTime accessDatetime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "service_accessed", nullable = false)
    private ServiceType serviceAccessed;
}