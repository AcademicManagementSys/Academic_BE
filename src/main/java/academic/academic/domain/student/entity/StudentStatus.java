package academic.academic.domain.student.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum StudentStatus {

    @JsonProperty("enrolled") ENROLLED,
    @JsonProperty("paused") PAUSED,
    @JsonProperty("withdrawn") WITHDRAWN
}
