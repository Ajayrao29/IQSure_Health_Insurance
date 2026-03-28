// Data Transfer Object for ResetPasswordRequest
package org.hartford.iqsure.dto.auth;
import lombok.Data;
@Data
public class ResetPasswordRequest {
    private String otp;
    private String newPassword;
}