// Data Transfer Object for ForgotPasswordRequest
package org.hartford.iqsure.dto.auth;
import lombok.Data;
@Data
public class ForgotPasswordRequest {
    private String email;
}