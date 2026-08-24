package academic.academic.global.exception;

import java.util.List;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, List<FieldError> details) {
    }

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(new ErrorBody(code.name(), message, null));
    }

    public static ErrorResponse of(ErrorCode code, String message, List<FieldError> details) {
        return new ErrorResponse(new ErrorBody(code.name(), message, details));
    }
}
