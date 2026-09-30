package com.smarthr.smarthr.role.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.smarthr.smarthr.role.entity.RoleEntity;
import com.smarthr.smarthr.common.exception.ResourceNotFoundException;
import com.smarthr.smarthr.common.exception.UserAlreadyExistsException;
import com.smarthr.smarthr.role.repository.RoleRepository;
import com.smarthr.smarthr.role.dto.RoleRequest;
import com.smarthr.smarthr.common.dto.PagedResponse;
import com.smarthr.smarthr.role.dto.RoleResponse;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        roleEntity = RoleEntity.builder()
                .id(1)
                .name("ROLE_ADMIN")
                .build();
    }

    @Test
    void createRole_Success() {
        RoleRequest request = RoleRequest.builder().name("ROLE_ADMIN").build();

        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(false);
        when(roleRepository.save(any(RoleEntity.class))).thenReturn(roleEntity);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("ROLE_ADMIN", response.getName());
    }

    @Test
    void createRole_BlankName_ThrowsException() {
        RoleRequest request = RoleRequest.builder().name("").build();

        assertThrows(IllegalArgumentException.class, () -> roleService.createRole(request));
    }

    @Test
    void createRole_AlreadyExists_ThrowsException() {
        RoleRequest request = RoleRequest.builder().name("ROLE_ADMIN").build();

        when(roleRepository.existsByName("ROLE_ADMIN")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> roleService.createRole(request));
    }

    @Test
    void getRoleById_Success() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(roleEntity));

        RoleResponse response = roleService.getRoleById(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("ROLE_ADMIN", response.getName());
    }

    @Test
    void getRoleById_NotFound_ThrowsException() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleById(99));
    }

    @Test
    void getRoleByName_Success() {
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleEntity));

        RoleResponse response = roleService.getRoleByName("ROLE_ADMIN");

        assertNotNull(response);
        assertEquals("ROLE_ADMIN", response.getName());
    }

    @Test
    void getAllRoles_Success() {
        when(roleRepository.findAll()).thenReturn(List.of(roleEntity));

        List<RoleResponse> responses = roleService.getAllRoles();

        assertEquals(1, responses.size());
        assertEquals("ROLE_ADMIN", responses.get(0).getName());
    }

    @Test
    void getAllRolesPaged_Success() {
        Page<RoleEntity> page = new PageImpl<>(List.of(roleEntity));
        PageRequest pageable = PageRequest.of(0, 10);
        when(roleRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<RoleResponse> pagedResponse = roleService.getAllRoles(pageable);

        assertNotNull(pagedResponse);
        assertEquals(1, pagedResponse.getContent().size());
        assertEquals("ROLE_ADMIN", pagedResponse.getContent().get(0).getName());
    }

    @Test
    void deleteRole_Success() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(roleEntity));
        doNothing().when(roleRepository).delete(roleEntity);

        roleService.deleteRole(1);

        verify(roleRepository).delete(roleEntity);
    }
}
