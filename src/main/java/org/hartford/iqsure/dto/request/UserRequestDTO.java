// Data Transfer Object for UserRequestDTO

package org.hartford.iqsure.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class UserRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    private String phone;
    private String role;
    private String licenseNumber;
    private String specialization;
    private java.math.BigDecimal commissionPercentage;
    private String employeeId;
    private String department;
    private java.math.BigDecimal approvalLimit;
    private String city;
    private String state;
    private String zipCode;
}