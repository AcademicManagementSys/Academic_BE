package academic.academic.domain.user.dto;

import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        Role role,
        String loginId,
        String phone,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getLoginId(),
                user.getPhone(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
