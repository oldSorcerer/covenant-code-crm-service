package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudentSpecifications;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return studentRepository.findById(id)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден с ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream()
                .map(studentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        User user = null;

        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

            if (studentRepository.existsByUser_Id(request.getUserId())) {
                throw new ConflictException(
                        String.format("Пользователь с id %d уже привязан к другому студенту", request.getUserId())
                );
            }
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .user(user)
                .build();

        Student savedStudent = studentRepository.saveAndFlush(student);

        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAll(String search, Pageable pageable) {
        Specification<Student> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and(StudentSpecifications.searchByText(search));
        }
        return studentRepository.findAll(spec, pageable)
                .map(studentMapper::toResponse);
    }
}
