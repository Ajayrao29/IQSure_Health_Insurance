// Controller handling AuthController related API endpoints

// Controller for user authentication and account registration
package org.hartford.iqsure.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.auth.AuthRequest;
import org.hartford.iqsure.dto.auth.AuthResponse;
import org.hartford.iqsure.dto.request.UserRequestDTO;
import org.hartford.iqsure.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register and Login")
public class AuthController {
    private final UserService userService;
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(dto));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody org.hartford.iqsure.dto.auth.ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody org.hartford.iqsure.dto.auth.ResetPasswordRequest request) {
        userService.resetPassword(request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}