package academic.academic.domain.notice.dto;

import academic.academic.domain.notice.entity.Notice;
import academic.academic.domain.notice.entity.NoticeScope;
import academic.academic.domain.schoolclass.entity.SchoolClass;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        Long authorId,
        String authorName,
        NoticeScope scope,
        Long classId,
        String className,
        String title,
        String content,
        boolean isPinned,
        LocalDateTime createdAt
) {
    public static NoticeResponse from(Notice notice) {
        SchoolClass schoolClass = notice.getSchoolClass();
        return new NoticeResponse(
                notice.getId(),
                notice.getAuthor().getId(),
                notice.getAuthor().getName(),
                notice.getScope(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.getCreatedAt()
        );
    }
}
