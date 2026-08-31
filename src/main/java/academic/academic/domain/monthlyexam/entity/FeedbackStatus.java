package academic.academic.domain.monthlyexam.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FeedbackStatus {

    @JsonProperty("strength") STRENGTH,
    @JsonProperty("needsWork") NEEDS_WORK
}
