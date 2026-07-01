package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper
public interface TeacherMapper {

    TeacherResponse toResponse(User user);

    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }

}
