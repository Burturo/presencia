package com.example.presencia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String departmentName;
}
