package academic.academic.domain.notice.dto;

import academic.academic.domain.notice.entity.NoticeScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 공지 작성 (FR-09-01, FR-09-02). scope가 class이면 classId가 필수다.
 */
public record NoticeCreateRequest(
        @NotNull Long authorId,
        @NotNull NoticeScope scope,
        Long classId,
        @NotBlank String title,
        @NotBlank String content,
        Boolean isPinned
) {
}
