package com.cabbooking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDriver(
            @Valid @RequestBody DriverRegistrationRequest request) {

        try {
            Driver driver = driverService.registerDriver(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Driver registration successful",
                            "driverId", driver.getId(),
                            "userId", driver.getUser().getId(),
                            "fullName", driver.getUser().getFullName(),
                            "email", driver.getUser().getEmail(),
                            "vehicleModel", driver.getVehicleModel(),
                            "vehicleNumber", driver.getVehicleNumber(),
                            "available", driver.isAvailable()
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );
        }
    }
}