package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.mapper.TeacherMapper;
import com.covenantcode.crm.repository.TeacherSpecifications;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final UserRepository userRepository;
    private final TeacherMapper teacherMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAll(String search, Pageable pageable) {
        Specification<User> spec = TeacherSpecifications.hasRole(RoleName.TEACHER);

        if (StringUtils.hasText(search)) {
            spec = spec.and(TeacherSpecifications.searchByText(search));
        }

        return userRepository.findAll(spec, pageable)
                .map(teacherMapper::toResponse);
    }
}
