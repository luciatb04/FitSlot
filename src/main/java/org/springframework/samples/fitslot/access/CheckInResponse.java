package org.springframework.samples.fitslot.access;

import java.time.LocalDateTime;

public record CheckInResponse(
    Integer accessRecordId,
    Integer memberId,
    LocalDateTime accessDatetime,
    String serviceAccessed,
    String result
) {
}
