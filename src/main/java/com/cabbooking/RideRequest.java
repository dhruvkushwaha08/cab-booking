package com.cabbooking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record RideRequest(

        @NotBlank(message = "Pickup location is required")
        @Size(max = 255)
        String pickupLocation,

        @NotBlank(message = "Drop-off location is required")
        @Size(max = 255)
        String dropoffLocation,

        @NotNull(message = "Pickup latitude is required")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double pickupLatitude,

        @NotNull(message = "Pickup longitude is required")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double pickupLongitude,

        @NotNull(message = "Drop-off latitude is required")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double dropoffLatitude,

        @NotNull(message = "Drop-off longitude is required")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double dropoffLongitude

) {}