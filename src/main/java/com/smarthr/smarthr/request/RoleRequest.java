package com.smarthr.smarthr.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class RoleRequest {

    private String name;
    private String roleName;

    public String getName() {
        if (name != null && !name.isBlank()) {
            return name.toUpperCase();
        }
        return roleName.toUpperCase();
    }
}
