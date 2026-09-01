package academic.academic.domain.notice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NoticeScope {

    @JsonProperty("all") ALL,
    @JsonProperty("class") CLASS
}
