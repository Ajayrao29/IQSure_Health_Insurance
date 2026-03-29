// Data Transfer Object for UserResponseDTO

package org.hartford.iqsure.dto.response;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Integer userPoints;
    private String role;
    private Integer totalQuizzesTaken;
    private Integer currentStreak;
    private Integer experiencePoints;
    private String rank;
    private Integer fortressIntegrity;
    private String licenseNumber;
    private String specialization;
    private java.math.BigDecimal commissionPercentage;
    private Integer totalQuotesSent;
    private String employeeId;
    private String department;
    private java.math.BigDecimal approvalLimit;
    private Integer totalClaimsProcessed;
    private Integer totalClaimsApproved;
    private Integer totalClaimsRejected;
    private String city;
    private String state;
    private String zipCode;
    private String status;
}