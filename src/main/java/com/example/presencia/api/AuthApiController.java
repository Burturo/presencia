package com.example.presencia.api;

import com.example.presencia.dto.request.LoginRequest;
import com.example.presencia.dto.response.MobileAuthResponse;
import com.example.presencia.dto.response.UserResponse;
import com.example.presencia.model.User;
import com.example.presencia.security.jwt.JwtTokenProvider;
import com.example.presencia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<MobileAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByIdentifier(request.getMatricule());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return ResponseEntity.ok(MobileAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserResponse.fromUser(user))
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<MobileAuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userService.findByEmail(email);

        String newAccessToken = jwtTokenProvider.generateToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return ResponseEntity.ok(MobileAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(UserResponse.fromUser(user))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Deconnexion reussie"));
    }
}
