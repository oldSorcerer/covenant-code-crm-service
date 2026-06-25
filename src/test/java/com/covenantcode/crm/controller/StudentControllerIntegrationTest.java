package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @AfterEach
    void tearDown() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Тест 1: успешное создание студента без привязки к пользователю (роль ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturnCreated_WhenValidRequestNoUserId() throws Exception {

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email("ivan@test.com")
                .phone("123456789")
                .birthDate(LocalDate.of(2000, 1, 1))
                .userId(null)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(nullValue())))
                .andExpect(jsonPath("$.firstName", is("Иван")));

        assertThat(studentRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Тест 2: ошибка 404, если указанный userId не существует")
    @WithMockUser(roles = "MANAGER")
    void create_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {

        Long nonExistentUserId = 999L;
        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Петр")
                .lastName("Петров")
                .userId(nonExistentUserId)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("resource-not-found")));
    }

    @Test
    @DisplayName("Тест 3: ошибка 409, если пользователь уже привязан к другому студенту")
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturnConflict_WhenUserAlreadyLinked() throws Exception {

        Role studentRole = roleRepository.findByName(RoleName.STUDENT).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name(RoleName.STUDENT)
                        .build()));

        User existingUser = userRepository.save(User.builder()
                .firstName("User")
                .lastName("X")
                .email("user-x@test.com")
                .password("password")
                .role(studentRole)
                .enabled(true)
                .build());

        studentRepository.save(Student.builder()
                .firstName("Уже")
                .lastName("Существующий")
                .user(existingUser)
                .build());

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Новый")
                .lastName("Студент")
                .userId(existingUser.getId())
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("conflict")));
    }

    @Test
    @DisplayName("Тест 4: ошибка 403, если студент создается пользователем с ролью TEACHER")
    @WithMockUser(roles = "TEACHER")
    void create_ShouldReturnForbidden_WhenRoleIsTeacher() throws Exception {

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Access")
                .lastName("Denied")
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
