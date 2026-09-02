package academic.academic.global.security;

import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 파라미터에 {@link JwtAuthenticationFilter}가 request attribute로 심어둔
 * {@link AuthenticatedUser}를 주입한다. attribute가 없다면(필터를 통과하지 못한 요청이라는 뜻이므로
 * 정상 흐름에서는 발생하지 않아야 하지만) 방어적으로 401을 던진다.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object user = webRequest.getAttribute(JwtAuthenticationFilter.CURRENT_USER_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
        }
        return authenticatedUser;
    }
}
