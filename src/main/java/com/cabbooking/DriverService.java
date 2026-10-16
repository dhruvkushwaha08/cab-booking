package com.cabbooking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private final AuthService authService;
    private final DriverRepository driverRepository;

    public DriverService(
            AuthService authService,
            DriverRepository driverRepository) {
        this.authService = authService;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public Driver registerDriver(DriverRegistrationRequest request) {

        if (request.licenseNumber() == null
                || request.licenseNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Driving licence number is required.");
        }

        if (request.vehicleModel() == null
                || request.vehicleModel().isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle model is required.");
        }

        if (request.vehicleNumber() == null
                || request.vehicleNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "Vehicle number is required.");
        }

        String licenseNumber = request.licenseNumber().trim();
        String vehicleNumber = request.vehicleNumber()
                .trim().toUpperCase(java.util.Locale.ROOT);

        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new IllegalArgumentException(
                    "This driving licence is already registered.");
        }

        if (driverRepository.existsByVehicleNumber(vehicleNumber)) {
            throw new IllegalArgumentException(
                    "This vehicle number is already registered.");
        }

        User user = authService.registerUser(
                request.fullName(),
                request.email(),
                request.password(),
                User.Role.DRIVER
        );

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setLicenseNumber(licenseNumber);
        driver.setVehicleModel(request.vehicleModel().trim());
        driver.setVehicleNumber(vehicleNumber);
        driver.setVehicleColor(
                request.vehicleColor() == null
                        ? null
                        : request.vehicleColor().trim()
        );

        return driverRepository.save(driver);
    }
}