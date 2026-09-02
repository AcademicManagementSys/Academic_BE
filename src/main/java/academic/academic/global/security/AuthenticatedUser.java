package academic.academic.global.security;

import academic.academic.domain.user.entity.Role;

/**
 * JWT Access Token에서 검증을 마치고 뽑아낸 요청자 신원. 컨트롤러는 {@link CurrentUser}로 이 값을
 * 주입받아 클라이언트가 보낸 id 대신 이 값(me.id())을 신뢰한다.
 */
public record AuthenticatedUser(Long id, Role role, String loginId) {
}
