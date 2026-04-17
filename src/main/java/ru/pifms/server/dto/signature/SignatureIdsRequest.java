package ru.pifms.server.dto.signature;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignatureIdsRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<@NotNull(message = "id must not be null") UUID> ids;
}
