package academic.academic.domain.student.dto;

/**
 * 학생 등록 시 발급된 로그인 계정 정보 (FR-01-07). tempPassword는 등록 응답에 한해 1회 노출된다.
 */
public record StudentAccountInfo(
        Long userId,
        String loginId,
        String tempPassword
) {
}
