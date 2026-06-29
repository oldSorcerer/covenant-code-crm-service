package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    @DisplayName("Тест 1: Успешное создание студента без привязки к пользователю")
    void create_WithoutUserId_ShouldSucceed() {

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .userId(null)
                .build();

        Student savedStudent = Student.builder().id(1L).firstName("Ivan").build();
        StudentResponse expectedResponse = new StudentResponse();
        expectedResponse.setId(1L);
        expectedResponse.setUserId(null);

        when(studentRepository.saveAndFlush(any(Student.class))).thenReturn(savedStudent);
        when(studentMapper.toResponse(savedStudent)).thenReturn(expectedResponse);

        StudentResponse actualResponse = studentService.create(request);

        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertNull(actualResponse.getUserId());

        verify(userRepository, never()).findById(any());
        verify(studentRepository).saveAndFlush(any(Student.class));
    }

    @Test
    @DisplayName("Тест 2: Успешное создание студента с валидным userId")
    void create_WithValidUserId_ShouldSucceed() {

        Long userId = 5L;
        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Petr")
                .userId(userId)
                .build();

        User user = new User();
        user.setId(userId);

        Student savedStudent = Student.builder().id(10L).user(user).build();
        StudentResponse expectedResponse = new StudentResponse();
        expectedResponse.setId(10L);
        expectedResponse.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.existsByUser_Id(userId)).thenReturn(false);
        when(studentRepository.saveAndFlush(any(Student.class))).thenReturn(savedStudent);
        when(studentMapper.toResponse(savedStudent)).thenReturn(expectedResponse);

        StudentResponse actualResponse = studentService.create(request);

        assertEquals(userId, actualResponse.getUserId());
        verify(userRepository).findById(userId);
        verify(studentRepository).existsByUser_Id(userId);
    }

    @Test
    @DisplayName("Тест 3: Ошибка, если указанный userId не существует")
    void create_WithNonExistentUserId_ShouldThrowNotFound() {

        Long userId = 99L;
        StudentCreateRequest request = StudentCreateRequest.builder().userId(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.create(request));

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 4: Ошибка, если пользователь уже привязан к другому студенту")
    void create_WithAlreadyTakenUserId_ShouldThrowConflict() {

        Long userId = 5L;
        StudentCreateRequest request = StudentCreateRequest.builder().userId(userId).build();

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.existsByUser_Id(userId)).thenReturn(true);


        assertThrows(ConflictException.class, () -> studentService.create(request));

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Список всех студентов без поиска — возвращает всех студентов постранично")
    void getAll_WithoutSearch_ShouldReturnAllStudentsPaginated() {
        Pageable pageable = PageRequest.of(0, 20);

        Student student1 = new Student();
        student1.setId(1L);
        student1.setFirstName("Алиса");
        student1.setLastName("Смирнова");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setFirstName("Борис");
        student2.setLastName("Иванов");

        List<Student> students = List.of(student1, student2);
        Page<Student> studentPage = new PageImpl<>(students, pageable, students.size());

        StudentResponse response1 = new StudentResponse();
        response1.setId(1L);
        response1.setFirstName("Алиса");
        response1.setLastName("Смирнова");

        StudentResponse response2 = new StudentResponse();
        response2.setId(2L);
        response2.setFirstName("Борис");
        response2.setLastName("Иванов");

        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(studentPage);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        Page<StudentResponse> result = studentService.getAll(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(response1, response2);

        ArgumentCaptor<Specification<Student>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(studentRepository, times(1)).findAll(specCaptor.capture(), eq(pageable));

        Specification<Student> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
    }

    @Test
    @DisplayName("Поиск по строке — возвращает отфильтрованных студентов")
    void getAll_WithSearch_ShouldReturnFilteredStudents() {
        String search = "Смир";
        Pageable pageable = PageRequest.of(0, 20);

        Student student1 = new Student();
        student1.setId(1L);
        student1.setFirstName("Алиса");
        student1.setLastName("Смирнова");
        student1.setPhone("+79161234567");
        student1.setEmail("alice@example.com");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setFirstName("Петр");
        student2.setLastName("Смирнов");
        student2.setPhone("+79161112233");
        student2.setEmail("petr@example.com");

        List<Student> students = List.of(student1, student2);
        Page<Student> studentPage = new PageImpl<>(students, pageable, students.size());

        StudentResponse response1 = new StudentResponse();
        response1.setId(1L);
        response1.setFirstName("Алиса");
        response1.setLastName("Смирнова");

        StudentResponse response2 = new StudentResponse();
        response2.setId(2L);
        response2.setFirstName("Петр");
        response2.setLastName("Смирнов");

        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(studentPage);
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        Page<StudentResponse> result = studentService.getAll(search, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().contains(response1));
        assertTrue(result.getContent().contains(response2));

        result.getContent().forEach(student ->
                assertTrue(student.getLastName().contains("Смир"))
        );

        ArgumentCaptor<Specification<Student>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(studentRepository, times(1)).findAll(specCaptor.capture(), eq(pageable));

        Specification<Student> capturedSpec = specCaptor.getValue();
        assertNotNull(capturedSpec, "Спецификация не должна быть null при переданном search");
    }

    @Test
    @DisplayName("Пустой список студентов — возвращает пустую страницу")
    void getAll_WhenNoStudents_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);

        List<Student> emptyList = List.of();
        Page<Student> emptyPage = new PageImpl<>(emptyList, pageable, 0);

        when(studentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<StudentResponse> result = studentService.getAll(null, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(20, result.getSize());

        verify(studentRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(studentMapper, never()).toResponse(any(Student.class));
    }
}
