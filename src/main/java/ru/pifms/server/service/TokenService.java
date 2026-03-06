package ru.pifms.server.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import ru.pifms.server.config.JwtTokenProvider;
import ru.pifms.server.dto.LoginDTO;
import ru.pifms.server.dto.TokenPairDTO;
import ru.pifms.server.entity.Role;
import ru.pifms.server.entity.SessionStatus;
import ru.pifms.server.entity.User;
import ru.pifms.server.entity.UserSession;
import ru.pifms.server.repository.UserRepository;
import ru.pifms.server.repository.UserSessionsRepository;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final UserRepository userRepository;
    private final UserSessionsRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final SessionAdminService sessionAdminService;


    @Transactional
    public TokenPairDTO loginIssueTokens(LoginDTO req, String deviceId) {
        User u = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }

        Map<String,Object> accessClaims = new HashMap<>();
        accessClaims.put("roles", u.getRoles().stream()
            .map(Role::getName)
            .map(Role.RoleType::getDescription)
            .toList());
        accessClaims.put("uid", u.getId());
        if (deviceId != null) accessClaims.put("deviceId", deviceId);

        Map<String,Object> refreshClaims = new HashMap<>();
        if (deviceId != null) refreshClaims.put("deviceId", deviceId);

        Instant accessExpiry = jwt.calculateAccessTokenExpiry();
        Instant refreshExpiry = jwt.calculateRefreshTokenExpiry();
        
        String access = jwt.generateAccessToken(u.getUsername(), accessClaims);
        String refresh = jwt.generateRefreshToken(u.getUsername(), refreshClaims);

        UserSession sess = UserSession.builder()
            .userEmail(u.getEmail())
            .deviceId(deviceId)
            .accessToken(access)
            .refreshToken(refresh)
            .accessTokenExpiry(accessExpiry)
            .refreshTokenExpiry(refreshExpiry)
            .status(SessionStatus.ACTIVE)
            .build();
        sessionRepository.save(sess);

        return new TokenPairDTO(access, refresh);
    }


    @Transactional
    public TokenPairDTO refreshTokens(String refreshToken) {
        Jws<Claims> jws;
        try {
            jws = jwt.parseAndValidate(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!jwt.isRefreshToken(jws)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong token type");
        }

        UserSession sess = sessionRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session not found"));

        if (sess.getStatus() == SessionStatus.USED) {
            sessionAdminService.revokeByToken(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has already been used");
        }

        if (sess.getStatus() != SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is not active");
        }

        if (sess.getRefreshTokenExpiry().isBefore(Instant.now())) {
            sessionAdminService.revokeByToken(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }


        sess.setStatus(SessionStatus.USED);
        sessionRepository.save(sess);


        String subject = jws.getBody().getSubject();
        User u = userRepository.findByUsername(subject)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String deviceId = (String) jws.getBody().get("deviceId");


        Map<String,Object> accessClaims = new HashMap<>();
        accessClaims.put("roles", u.getRoles().stream()
            .map(Role::getName)
            .map(Role.RoleType::getDescription)
            .toList());
        accessClaims.put("uid", u.getId());
        if (deviceId != null) accessClaims.put("deviceId", deviceId);


        Map<String,Object> refreshClaims = new HashMap<>();
        if (deviceId != null) refreshClaims.put("deviceId", deviceId);


        Instant newAccessExpiry = jwt.calculateAccessTokenExpiry();
        Instant newRefreshExpiry = jwt.calculateRefreshTokenExpiry();
        

        String newAccess  = jwt.generateAccessToken(subject, accessClaims);
        String newRefresh = jwt.generateRefreshToken(subject, refreshClaims);

        
        UserSession newSess = UserSession.builder()
            .userEmail(u.getEmail())
            .deviceId(deviceId)
            .accessToken(newAccess)
            .refreshToken(newRefresh)
            .accessTokenExpiry(newAccessExpiry)
            .refreshTokenExpiry(newRefreshExpiry)
            .status(SessionStatus.ACTIVE)
            .build();
        sessionRepository.save(newSess);

        return new TokenPairDTO(newAccess, newRefresh);
    }
}
