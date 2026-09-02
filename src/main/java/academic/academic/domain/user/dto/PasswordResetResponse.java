package academic.academic.domain.user.dto;

/**
 * 비밀번호 초기화 응답 (FR-01-07). tempPassword는 이 응답에 한해 1회 노출된다.
 */
public record PasswordResetResponse(
        Long userId,
        String loginId,
        String tempPassword,
        boolean mustChangePassword
) {
}
