package academic.academic.domain.notice.dto;

import academic.academic.domain.notice.entity.NoticeScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 공지 작성 (FR-09-01, FR-09-02). scope가 class이면 classId가 필수다. 작성자는 로그인 토큰에서
 * 가져오므로(@CurrentUser) 요청 본문에 authorId를 받지 않는다.
 */
public record NoticeCreateRequest(
        @NotNull NoticeScope scope,
        Long classId,
        @NotBlank String title,
        @NotBlank String content,
        Boolean isPinned
) {
}
