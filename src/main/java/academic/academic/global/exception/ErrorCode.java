package academic.academic.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),
    FORBIDDEN_SCOPE(HttpStatus.FORBIDDEN),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
