package ru.pifms.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.pifms.server.entity.UserSession;

@Repository
public interface UserSessionsRepository extends JpaRepository<UserSession, java.util.UUID> {
    Optional<UserSession> findByRefreshToken(String refreshToken);
}
