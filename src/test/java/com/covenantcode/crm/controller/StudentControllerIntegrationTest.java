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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @Test
    @DisplayName("GET /api/v1/students — список всех студентов (200)")
    @WithMockUser(roles = "MANAGER")
    void getAllStudents_WithoutFilters_ShouldReturnAllStudents() throws Exception {
        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Иванов")
                .phone("+79161112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        student1.getId().intValue(),
                        student2.getId().intValue(),
                        student3.getId().intValue()
                )))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Борис", "Екатерина")))
                .andExpect(jsonPath("$.content[*].lastName", containsInAnyOrder("Смирнова", "Иванов", "Петрова")))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/students?search=Смир — поиск по частичному имени (200)")
    @WithMockUser(roles = "MANAGER")
    void getAllStudents_SearchByPartialName_ShouldReturnFilteredStudents() throws Exception {
        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Смирнов")
                .phone("+79161112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        Student student4 = Student.builder()
                .firstName("Дмитрий")
                .lastName("Смирновский")
                .phone("+79165554433")
                .email("dmitry@example.com")
                .birthDate(LocalDate.of(1998, 11, 25))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3, student4));

        mockMvc.perform(get("/api/v1/students")
                        .param("search", "Смир")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].lastName", containsInAnyOrder("Смирнова", "Смирнов", "Смирновский")))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Борис", "Дмитрий")))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        student1.getId().intValue(),
                        student2.getId().intValue(),
                        student4.getId().intValue()
                )))
                .andExpect(jsonPath("$.content[*].lastName", not(containsString("Петрова"))));
    }

    @Test
    @DisplayName("GET /api/v1/students?search=7916 — поиск по телефону (200)")
    @WithMockUser(roles = "MANAGER")
    void getAllStudents_SearchByPhone_ShouldReturnFilteredStudents() throws Exception {
        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Иванов")
                .phone("+79261112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3));

        mockMvc.perform(get("/api/v1/students")
                        .param("search", "7916")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].phone", containsInAnyOrder("+79161234567", "+79169998877")))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Екатерина")))
                .andExpect(jsonPath("$.content[*].phone", not(containsString("+79261112233"))));
    }

    @Test
    @DisplayName("GET /api/v1/students — TEACHER не имеет доступа (403)")
    @WithMockUser(roles = "TEACHER")
    void getAllStudents_WithTeacherRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }
}
