package academic.academic.domain.test.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TestSubject {

    @JsonProperty("vocab") VOCAB,
    @JsonProperty("reading") READING,
    @JsonProperty("grammar") GRAMMAR,
    @JsonProperty("syntax") SYNTAX
}
