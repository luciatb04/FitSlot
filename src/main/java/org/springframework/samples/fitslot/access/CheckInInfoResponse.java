
package org.springframework.samples.fitslot.access;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.samples.fitslot.membership.ServiceType;
import org.springframework.samples.fitslot.membership.SubscriptionStatus;

public record CheckInInfoResponse(
    LocalDateTime checkedAt,
    MedicalCertificateInfo medicalCertificate,
    List<SubscriptionInfo> subscriptions
) {

    public record MedicalCertificateInfo(
        LocalDate expiryDate,
        String validity,
        String denialReason
    ) {}

    public record SubscriptionInfo(
        Integer id,
        String tierName,
        SubscriptionStatus status,
        SubscriptionStatus verificationStatus,
        String denialReason,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean timeSlotAllowed,
        String timeSlotDenialReason,
        Integer remainingEntries,
        Set<ServiceType> allowedServices
    ) {}
}
