package com.example.presencia.dto.response;

import com.example.presencia.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String matricule;
    private String role;
    private String photo;
    private String departement;
    private String poste;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .matricule(user.getMatricule())
                .role(user.getRole().name())
                .photo(user.getPhoto())
                .departement(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .poste(user.getPoste())
                .build();
    }
}
