package ru.pifms.server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import ru.pifms.server.entity.User;
import ru.pifms.server.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ApplicationUserService {

	private final UserRepository userRepository;

	public User getActiveUserOrFail(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found"));

		if (user.isDisabled() || user.isAccountExpired() || user.isAccountLocked() || user.isCredentialsExpired()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found");
		}

		return user;
	}

	public User getUserOrFail(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
	}

	public User getUserByUsernameOrFail(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
	}
}
