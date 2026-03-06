package ru.pifms.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.pifms.server.dto.LoginDTO;
import ru.pifms.server.dto.RefreshRequestDTO;
import ru.pifms.server.dto.RegisterDTO;
import ru.pifms.server.dto.TokenPairDTO;
import ru.pifms.server.dto.UserDTO;
import ru.pifms.server.service.AuthService;
import ru.pifms.server.service.TokenService;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterDTO request) {
        UserDTO userDTO = authService.register(request);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairDTO> login(@Valid @RequestBody LoginDTO request, @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        TokenPairDTO pair = tokenService.loginIssueTokens(request, deviceId);
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        TokenPairDTO pair = tokenService.refreshTokens(request.getRefreshToken());
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/{userId}/assign-role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> assignRole(@PathVariable Long userId, @PathVariable String roleName) {
        UserDTO userDTO = authService.assignRole(userId, roleName);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/{userId}/remove-role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        UserDTO userDTO = authService.removeRole(userId, roleName);
        return ResponseEntity.ok(userDTO);
    }
}
