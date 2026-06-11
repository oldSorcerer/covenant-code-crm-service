package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String managerToken;

    private User adminUser;
    private User managerUser;

    @BeforeEach
    void setUp() {

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ADMIN);
                    return roleRepository.save(role);
                });

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.MANAGER);
                    return roleRepository.save(role);
                });

        adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(adminRole)
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        managerUser = User.builder()
                .firstName("Manager")
                .lastName("User")
                .email("manager@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(managerRole)
                .enabled(true)
                .build();
        managerUser = userRepository.save(managerUser);

        adminToken = jwtService.generateToken(adminUser);
        managerToken = jwtService.generateToken(managerUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Disabled
    @Test
    public void getAllUsers_withAdminToken_success() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    public void getAllUsers_withManagerToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Disabled
    @Test
    public void getAllUsers_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN получает профиль другого пользователя — HTTP 200")
    void getUserById_adminGetsAnotherProfile_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", managerUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerUser.getId()))
                .andExpect(jsonPath("$.email").value(managerUser.getEmail()));
    }

    @Test
    @DisplayName("Пользователь получает свой профиль — HTTP 200")
    void getUserById_userGetsOwnProfile_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", managerUser.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerUser.getId()))
                .andExpect(jsonPath("$.email").value(managerUser.getEmail()));
    }

    @Test
    @DisplayName("MANAGER запрашивает чужой профиль — HTTP 403")
    void getUserById_managerGetsAnotherProfile_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", adminUser.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"));
    }

    @Test
    @DisplayName("Несуществующий ID (ADMIN) — HTTP 404")
    void getUserById_adminGetsMissingProfile_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }
}