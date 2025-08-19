package com.aplazo.challenge.aplazo_fullstack_challenge.dto.customer;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.aplazo.challenge.aplazo_fullstack_challenge.validation.Adult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El nombre solo puede contener letras")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El apellido solo puede contener letras")
    private String lastName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    @Pattern(regexp = "^[\\p{L}]+$", message = "El segundo apellido solo puede contener letras")
    private String secondLastName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @Adult
    private LocalDate dateOfBirth;

}
