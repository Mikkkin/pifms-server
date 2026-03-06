package ru.pifms.server.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "user_sessions",
       indexes = {
         @Index(name="ix_user_sessions_email", columnList="userEmail"),
         @Index(name="ix_user_sessions_refresh", columnList="refreshToken", unique = true)
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String userEmail;

    @Column(length = 100)
    private String deviceId;

    @Column(length = 800)
    private String accessToken;

    @Column(length = 800, nullable = false, unique = true)
    private String refreshToken;

    @Column(nullable = false)
    private Instant accessTokenExpiry;

    @Column(nullable = false)
    private Instant refreshTokenExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;
}
