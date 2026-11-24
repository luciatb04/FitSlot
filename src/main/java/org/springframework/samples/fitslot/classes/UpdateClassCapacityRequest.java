package org.springframework.samples.fitslot.classes;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateClassCapacityRequest(
        @NotNull @Min(1) Integer maxCapacity) {
}
