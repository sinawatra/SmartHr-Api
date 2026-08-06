package com.smarthr.smarthr.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating a Role.
 * Supports both 'name' and 'roleName' fields for backwards compatibility.
 *
 * @author sinawatrarith
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
@Setter
public class RoleRequest {
    private String name;

}
