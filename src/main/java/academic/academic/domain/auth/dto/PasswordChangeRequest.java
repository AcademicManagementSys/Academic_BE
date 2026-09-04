package academic.academic.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인한 사용자 본인이 현재 비밀번호를 확인하고 새 비밀번호로 바꾸는 요청.
 * 비밀번호를 완전히 잊어버려 현재 비밀번호를 모르는 경우는 관리자 강제 초기화
 * ({@link academic.academic.domain.user.service.UserService#resetPassword})를 쓴다.
 */
public record PasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword
) {
}
