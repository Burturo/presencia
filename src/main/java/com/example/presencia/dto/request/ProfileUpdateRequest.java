package com.example.presencia.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    @Email @NotBlank
    private String email;
}
