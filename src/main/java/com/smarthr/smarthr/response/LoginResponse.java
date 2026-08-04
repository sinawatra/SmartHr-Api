package com.smarthr.smarthr.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String token;
    // @Builder.Default
    // private String tokenType = "Bearer";
}
