package academic.academic.domain.auth.dto;

import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;

/**
 * 로그인 응답에 포함되는 사용자 요약 (API_명세서_v1.3 §2). hasMultipleChildren이 true면
 * 프런트엔드는 홈 대신 자녀 선택(SCR-17)으로 먼저 이동한다.
 *
 * <p>studentId는 role이 STUDENT일 때만 채워진다 — 학생 본인 로그인 계정이 자신의 studentId를
 * 알아낼 다른 방법이 없어서(/students/{id}/* 계열 API 전부 URL에 studentId가 필요) 로그인 시점에
 * 함께 내려준다. Student.userId(본인 계정)와 매핑되는 studentId를 찾아 채운다.</p>
 */
public record UserSummary(Long id, String name, Role role, boolean hasMultipleChildren, Long studentId) {

    public static UserSummary of(User user, boolean hasMultipleChildren) {
        return of(user, hasMultipleChildren, null);
    }

    public static UserSummary of(User user, boolean hasMultipleChildren, Long studentId) {
        return new UserSummary(user.getId(), user.getName(), user.getRole(), hasMultipleChildren, studentId);
    }
}
