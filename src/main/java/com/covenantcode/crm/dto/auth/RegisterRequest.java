package com.covenantcode.crm.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterRequest {
    @NotBlank(message = "Имя обязательно")
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    private String lastName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    private String password;

    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    private String phone;
}
