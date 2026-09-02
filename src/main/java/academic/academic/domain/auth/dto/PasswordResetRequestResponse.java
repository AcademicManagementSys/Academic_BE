package academic.academic.domain.auth.dto;

import java.time.LocalDateTime;

/**
 * 이메일 발송 인프라가 없어 발급된 재설정 토큰을 응답으로 직접 내려준다.
 * 실제 서비스라면 이 토큰은 이메일로만 전달되어야 하며 API 응답에 노출되면 안 된다 — 개발/테스트
 * 편의를 위한 임시 조치임을 명시한다 (Document/스펙_변경_제안.md 참고).
 */
public record PasswordResetRequestResponse(String resetToken, LocalDateTime expiresAt) {
}
