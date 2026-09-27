package com.example.svgmanager.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignVehicleConfigurationsRequest(
        @NotNull(message = "vehicleConfigurationIds list is required")
        List<Long> vehicleConfigurationIds
) {
}
