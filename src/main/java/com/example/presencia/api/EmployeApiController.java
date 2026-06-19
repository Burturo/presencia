package com.example.presencia.api;

import com.example.presencia.dto.request.ChangePasswordRequest;
import com.example.presencia.dto.request.ProfileUpdateRequest;
import com.example.presencia.dto.response.UserResponse;
import com.example.presencia.model.User;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employe")
@RequiredArgsConstructor
public class EmployeApiController {

    private final UserService userService;

    @GetMapping("/profil")
    public ResponseEntity<UserResponse> getProfil(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping("/profil/update")
    public ResponseEntity<UserResponse> updateProfil(@AuthenticationPrincipal UserDetails userDetails,
                                                      @Valid @RequestBody ProfileUpdateRequest request) {
        User user = userService.updateProfile(
                userDetails.getUsername(), request.getNom(), request.getPrenom(), request.getEmail());
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping("/profil/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifie avec succes"));
    }
}
