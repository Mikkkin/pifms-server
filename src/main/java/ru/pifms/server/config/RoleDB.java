package ru.pifms.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ru.pifms.server.entity.Role;
import ru.pifms.server.entity.Role.RoleType;
import ru.pifms.server.repository.RoleRepository;

@Component
@RequiredArgsConstructor
public class RoleDB implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = new Role();
                role.setName(roleType);
                role.setDescription(roleType.getDescription());
                roleRepository.save(role);
            }
        }
    }
}
