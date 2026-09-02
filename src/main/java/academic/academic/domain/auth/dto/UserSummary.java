package academic.academic.domain.auth.dto;

import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;

/**
 * 로그인 응답에 포함되는 사용자 요약 (API_명세서_V2 §3). hasMultipleChildren이 true면
 * 프런트엔드는 홈 대신 자녀 선택(SCR-17)으로 먼저 이동한다.
 */
public record UserSummary(Long id, String name, Role role, boolean hasMultipleChildren) {

    public static UserSummary of(User user, boolean hasMultipleChildren) {
        return new UserSummary(user.getId(), user.getName(), user.getRole(), hasMultipleChildren);
    }
}
