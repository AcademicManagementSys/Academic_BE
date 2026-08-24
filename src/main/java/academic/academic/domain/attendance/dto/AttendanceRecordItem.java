package academic.academic.domain.attendance.dto;

import academic.academic.domain.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceRecordItem(
        @NotNull Long studentId,
        @NotNull AttendanceStatus status,
        String note
) {
}
