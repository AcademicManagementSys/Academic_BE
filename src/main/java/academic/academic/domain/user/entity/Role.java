package academic.academic.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {

    @JsonProperty("admin") ADMIN,
    @JsonProperty("teacher") TEACHER,
    @JsonProperty("parent") PARENT,
    @JsonProperty("student") STUDENT
}
