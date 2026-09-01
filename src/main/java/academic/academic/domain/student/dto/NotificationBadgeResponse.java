package academic.academic.domain.student.dto;

import java.time.LocalDateTime;

/**
 * 알림 배지 (FR-08-01). since 이후 새로 입력된 출석/숙제/테스트/월말모의고사 건수를 집계한다.
 */
public record NotificationBadgeResponse(
        LocalDateTime since,
        int attendanceCount,
        int homeworkCount,
        int testCount,
        int monthlyExamCount,
        int totalCount
) {
}
