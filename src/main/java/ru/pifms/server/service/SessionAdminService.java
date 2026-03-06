package ru.pifms.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.pifms.server.entity.SessionStatus;
import ru.pifms.server.repository.UserSessionsRepository;

@Service
@RequiredArgsConstructor
public class SessionAdminService {

    private final UserSessionsRepository sessionRepository;

    @Transactional
    public void revokeByToken(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken)
            .ifPresent(session -> {
                session.setStatus(SessionStatus.REVOKED);
                sessionRepository.save(session);
            });
    }
}
