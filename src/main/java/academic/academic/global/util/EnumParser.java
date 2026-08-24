package academic.academic.global.util;

import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.springframework.util.StringUtils;

public final class EnumParser {

    private EnumParser() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " 값이 올바르지 않습니다: " + value);
        }
    }
}
