package ru.pifms.server.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "roles",
    indexes = {
        @Index(name = "ix_roles_name", columnList = "name", unique = true)
    }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleType name;

    @Column(length = 255)
    private String description;

    public Role(RoleType name) {
        this.name = name;
    }

    public enum RoleType {
        role_admin("ADMIN"),
        role_hr("HR"),
        role_candidate("Candidate"),
        role_user("User");

        private final String description;

        RoleType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
