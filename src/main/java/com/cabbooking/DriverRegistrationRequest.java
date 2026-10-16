package com.cabbooking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverRegistrationRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        String password,

        @NotBlank(message = "Driving licence number is required")
        String licenseNumber,

        @NotBlank(message = "Vehicle model is required")
        String vehicleModel,

        @NotBlank(message = "Vehicle number is required")
        String vehicleNumber,

        String vehicleColor

) {
}