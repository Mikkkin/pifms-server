package ru.pifms.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

    @Convert(converter = RoleTypeConverter.class)
    @Column(nullable = false, unique = true, length = 50)
    private RoleType name;

    @Column(length = 255)
    private String description;

    public Role(RoleType name) {
        this.name = name;
    }

    public enum RoleType {
        ADMIN("role_admin", "ADMIN"),
        USER("role_user", "USER");

        private final String dbValue;
        private final String description;

        RoleType(String dbValue, String description) {
            this.dbValue = dbValue;
            this.description = description;
        }

        public String getDbValue() {
            return dbValue;
        }

        public String getDescription() {
            return description;
        }

        public static RoleType fromDbValue(String dbValue) {
            for (RoleType roleType : values()) {
                if (roleType.dbValue.equalsIgnoreCase(dbValue)) {
                    return roleType;
                }
            }
            throw new IllegalArgumentException("Unknown role db value: " + dbValue);
        }

        public static RoleType fromInput(String value) {
            for (RoleType roleType : values()) {
                if (roleType.name().equalsIgnoreCase(value)
                    || roleType.dbValue.equalsIgnoreCase(value)
                    || roleType.description.equalsIgnoreCase(value)) {
                    return roleType;
                }
            }
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }
}
