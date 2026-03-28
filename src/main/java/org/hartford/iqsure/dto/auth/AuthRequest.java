// Data Transfer Object for AuthRequest

package org.hartford.iqsure.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class AuthRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
}