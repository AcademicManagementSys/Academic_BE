package academic.academic.domain.student.dto;

/**
 * 학생 본인 로그인 계정 발급 옵션 (FR-01-07). 생략하거나 loginId를 비우면 서버가 자동 생성한다.
 */
public record StudentAccountRequest(
        String loginId,
        Boolean autoGenerateLoginId
) {
}
