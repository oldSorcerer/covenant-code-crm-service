package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TeacherController Integration Tests")
class TeacherControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String adminToken;
    private User testTeacher;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = Role.builder()
                .name(RoleName.ADMIN)
                .build();
        Role teacherRole = Role.builder()
                .name(RoleName.TEACHER)
                .build();
        roleRepository.saveAll(List.of(adminRole, teacherRole));

        User admin = User.builder()
                .firstName("Admin")
                .lastName("Adminov")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("+79161234560")
                .enabled(true)
                .role(adminRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(admin);

        testTeacher = User.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@school.ru")
                .password(passwordEncoder.encode("teacher123"))
                .phone("+79161234567")
                .enabled(true)
                .role(teacherRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(testTeacher);

        adminToken = jwtService.generateToken(admin);
    }

    @Test
    @DisplayName("GET /api/v1/teachers — возвращает 200 и список преподавателей")
    void getAllTeachers_shouldReturn200AndTeacherList() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[*].email")
                        .value(hasItem("ivan.petrov@school.ru")))
                .andExpect(jsonPath("$.content[*].firstName")
                        .value(hasItem("Иван")))
                .andExpect(jsonPath("$.content[*].lastName")
                        .value(hasItem("Петров")))
                .andExpect(jsonPath("$.content[*].enabled")
                        .value(hasItem(true)));
    }

    @Test
    @DisplayName("GET /api/v1/teachers?search=имя — возвращает только отфильтрованных преподавателей")
    void getAllTeachers_withSearch_shouldReturnFilteredTeachers() throws Exception {
        String search = "Анна";

        User secondTeacher = User.builder()
                .firstName("Анна")
                .lastName("Смирнова")
                .email("anna.smirnova@school.ru")
                .password(passwordEncoder.encode("teacher123"))
                .phone("+79161234568")
                .enabled(true)
                .role(roleRepository.findByName(RoleName.TEACHER).orElseThrow())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(secondTeacher);

        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", search)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content").value(hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Анна"))
                .andExpect(jsonPath("$.content[0].lastName").value("Смирнова"))
                .andExpect(jsonPath("$.content[0].email").value("anna.smirnova@school.ru"))
                .andExpect(jsonPath("$.content[*].firstName").value(not(hasItem("Иван"))))
                .andExpect(jsonPath("$.content[*].lastName").value(not(hasItem("Петров"))));
    }

    @Test
    @DisplayName("GET /api/v1/teachers с токеном STUDENT — возвращает 403 Forbidden")
    void getAllTeachers_withStudentToken_shouldReturn403() throws Exception {
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(RoleName.STUDENT)
                            .build();
                    return roleRepository.save(newRole);
                });

        User student = User.builder()
                .firstName("Student")
                .lastName("Studentov")
                .email("student@test.com")
                .password(passwordEncoder.encode("student123"))
                .phone("+79161234569")
                .enabled(true)
                .role(studentRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(student);

        String studentToken = jwtService.generateToken(student);

        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
