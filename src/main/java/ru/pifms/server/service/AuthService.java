package ru.pifms.server.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.pifms.server.dto.RegisterDTO;
import ru.pifms.server.dto.UserDTO;
import ru.pifms.server.entity.Role;
import ru.pifms.server.entity.Role.RoleType;
import ru.pifms.server.entity.User;
import ru.pifms.server.exception.UserAlreadyExist;
import ru.pifms.server.repository.RoleRepository;
import ru.pifms.server.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordValidator;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO register(RegisterDTO request) {
        passwordValidator.validatePassword(request.getPassword());
        passwordValidator.validatePasswordMatch(request.getPassword(), request.getPasswordConfirm());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExist(
                String.format("Username '%s' already exist", request.getUsername())
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExist(
                String.format("Email '%s' already exist", request.getEmail())
            );
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccountExpired(false);
        user.setAccountLocked(false);
        user.setCredentialsExpired(false);
        user.setDisabled(false);

        Role defaultRole = roleRepository.findByName(RoleType.USER)
            .orElseThrow(() -> new RuntimeException("Role user not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return mapUserToDTO(savedUser);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException(
                String.format("User '%s' not found", username)
            ));
        return mapUserToDTO(user);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                String.format("User with ID '%d' not found", id)
            ));
        return mapUserToDTO(user);
    }

    private UserDTO mapUserToDTO(User user) {
        Set<String> roleNames = new HashSet<>();
        for (Role role : user.getRoles()) {
            roleNames.add(role.getName().toString());
        }

        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(roleNames)
            .build();
    }

    @Transactional
    public UserDTO assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException(
                String.format("User with ID '%d' not found", userId)
            ));
        
        Role role = roleRepository.findByName(RoleType.fromInput(roleName))
            .orElseThrow(() -> new RuntimeException(
                String.format("Role '%s' not found", roleName)
            ));
        
        user.getRoles().add(role);
        userRepository.save(user);
        
        return mapUserToDTO(user);
    }

    @Transactional
    public UserDTO removeRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException(
                String.format("User with ID '%d' not found", userId)
            ));
        
        Role role = roleRepository.findByName(RoleType.fromInput(roleName))
            .orElseThrow(() -> new RuntimeException(
                String.format("Role '%s' not found", roleName)
            ));
        
        user.getRoles().remove(role);
        userRepository.save(user);
        
        return mapUserToDTO(user);
    }
}
