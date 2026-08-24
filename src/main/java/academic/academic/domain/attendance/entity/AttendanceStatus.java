package academic.academic.domain.attendance.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AttendanceStatus {

    @JsonProperty("present") PRESENT,
    @JsonProperty("late") LATE,
    @JsonProperty("absent") ABSENT,
    @JsonProperty("earlyLeave") EARLY_LEAVE,
    @JsonProperty("makeup") MAKEUP
}
