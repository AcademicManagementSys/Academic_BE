package academic.academic.domain.notice.dto;

public record NoticeUpdateRequest(
        String title,
        String content
) {
}
