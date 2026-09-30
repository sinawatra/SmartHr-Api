package com.smarthr.smarthr.auth.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.smarthr.smarthr.role.dto.RoleResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private Long id;
    private String username;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private RoleResponse role;
    private String token;
    private String refreshToken;
    // @Builder.Default
    // private String tokenType = "Bearer";
}
