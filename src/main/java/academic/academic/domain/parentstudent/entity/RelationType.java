package academic.academic.domain.parentstudent.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RelationType {

    @JsonProperty("father") FATHER,
    @JsonProperty("mother") MOTHER,
    @JsonProperty("other") OTHER
}
