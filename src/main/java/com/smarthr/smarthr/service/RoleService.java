package com.smarthr.smarthr.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smarthr.smarthr.entity.RoleEntity;
import com.smarthr.smarthr.exception.ResourceNotFoundException;
import com.smarthr.smarthr.exception.UserAlreadyExistsException;
import com.smarthr.smarthr.repository.RoleRepository;
import com.smarthr.smarthr.request.RoleRequest;
import com.smarthr.smarthr.response.PagedResponse;
import com.smarthr.smarthr.response.RoleResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service handling business logic for roles.
 *
 * @author sinawatrarith
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    /**
     * Create a new role.
     */
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        String roleName = request.getName();
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name is required");
        }

        roleName = roleName.trim();
        if (roleRepository.existsByName(roleName)) {
            throw new UserAlreadyExistsException("Role with name '" + roleName + "' already exists");
        }

        RoleEntity roleEntity = RoleEntity.builder()
                .name(roleName)
                .build();

        RoleEntity savedRole = roleRepository.save(roleEntity);
        return mapToResponse(savedRole);
    }

    /**
     * Get role by ID.
     */
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Integer id) {
        RoleEntity roleEntity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));
        return mapToResponse(roleEntity);
    }

    /**
     * Get role by name.
     */
    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String name) {
        RoleEntity roleEntity = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
        return mapToResponse(roleEntity);
    }

    /**
     * Get all roles without pagination.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get roles with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<RoleResponse> getAllRoles(Pageable pageable) {
        Page<RoleEntity> page = roleRepository.findAll(pageable);
        List<RoleResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PagedResponse.of(page, content);
    }

    /**
     * Update role by ID.
     */
    @Transactional
    public RoleResponse updateRole(Integer id, RoleRequest request) {
        RoleEntity roleEntity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));

        String newName = request.getName();
        if (newName != null && !newName.isBlank()) {
            newName = newName.trim();
            if (!newName.equalsIgnoreCase(roleEntity.getName()) && roleRepository.existsByName(newName)) {
                throw new UserAlreadyExistsException("Role with name '" + newName + "' already exists");
            }
            roleEntity.setName(newName);
        }

        RoleEntity updatedRole = roleRepository.save(roleEntity);
        return mapToResponse(updatedRole);
    }

    /**
     * Delete role by ID.
     */
    @Transactional
    public void deleteRole(Integer id) {
        RoleEntity roleEntity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));
        roleRepository.delete(roleEntity);
    }

    /**
     * Convert RoleEntity to RoleResponse DTO.
     */
    public RoleResponse mapToResponse(RoleEntity entity) {
        return RoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
