package academic.academic.domain.user.dto;

import academic.academic.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserCreateRequest(
        @NotNull Role role,
        @NotBlank String name,
        @NotBlank String loginId,
        @NotBlank String password,
        String phone,
        List<Long> classIds
) {
}
