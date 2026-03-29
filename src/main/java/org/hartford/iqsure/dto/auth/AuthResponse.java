// Data Transfer Object for AuthResponse

package org.hartford.iqsure.dto.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private Integer userPoints;
    private Integer totalQuizzesTaken;
    private Integer currentStreak;
    private Integer experiencePoints;
    private String rank;
    private Integer fortressIntegrity;
    private String licenseNumber;
    private String employeeId;
    private String department;
    private java.math.BigDecimal approvalLimit;
}