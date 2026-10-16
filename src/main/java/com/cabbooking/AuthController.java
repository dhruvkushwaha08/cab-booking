
package com.cabbooking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        if (request.role() == null
                || request.role() == User.Role.ADMIN) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            "Choose CUSTOMER or DRIVER to register."
                    )
            );
        }

        try {
            User user = authService.registerUser(
                    request.fullName(),
                    request.email(),
                    request.password(),
                    request.role()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Registration successful",
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "email", user.getEmail(),
                            "role", user.getRole().name()
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            LoginResponse response = authService.login(
                    request.email(),
                    request.password()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid email or password.")
            );
        }
    }

    public record RegisterRequest(
            String fullName,
            String email,
            String password,
            User.Role role
    ) {
    }
}
