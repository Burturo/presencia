package com.example.presencia.service;

import com.example.presencia.exception.ResourceNotFoundException;
import com.example.presencia.model.User;
import com.example.presencia.model.enums.Role;
import com.example.presencia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findEmployees() {
        return userRepository.findByRole(Role.EMPLOYE);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + email));
    }

    public User findByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve: " + matricule));
    }

    public User findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifiant requis");
        }
        if (identifier.contains("@")) {
            return findByEmail(identifier);
        }
        return findByMatricule(identifier);
    }

    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email deja utilise: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User update(Long id, User updated) {
        User user = findById(id);
        user.setPrenom(updated.getPrenom());
        user.setNom(updated.getNom());
        user.setEmail(updated.getEmail());
        user.setMatricule(updated.getMatricule());
        user.setPoste(updated.getPoste());
        user.setRole(updated.getRole());
        user.setDepartment(updated.getDepartment());
        user.setActive(updated.isActive());
        return userRepository.save(user);
    }

    public User updateProfile(String email, String nom, String prenom, String newEmail) {
        User user = findByEmail(email);
        if (!email.equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email deja utilise: " + newEmail);
        }
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public long countActiveEmployees() {
        return userRepository.countByActiveTrue();
    }
}
