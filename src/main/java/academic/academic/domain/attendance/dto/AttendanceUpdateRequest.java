package academic.academic.domain.attendance.dto;

import academic.academic.domain.attendance.entity.AttendanceStatus;

public record AttendanceUpdateRequest(
        AttendanceStatus status,
        String note
) {
}
