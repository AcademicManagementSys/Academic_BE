package academic.academic.domain.auth.controller;

import academic.academic.domain.auth.dto.LoginRequest;
import academic.academic.domain.auth.dto.LoginResponse;
import academic.academic.domain.auth.dto.PasswordChangeRequest;
import academic.academic.domain.auth.dto.RefreshRequest;
import academic.academic.domain.auth.dto.TokenPairResponse;
import academic.academic.domain.auth.service.AuthService;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (API_명세서_V2 §3, REQ-AUTH-01·06). {@code /v1/me}(현재 사용자 조회)는
 * {@link academic.academic.domain.user.controller.MeController}에 있다. 비밀번호를 잊어버린 경우
 * (REQ-AUTH-07)는 이메일 인프라가 없어 자가 재설정 대신 관리자 강제 초기화
 * ({@link academic.academic.domain.user.controller.UserController#resetPassword})만 지원한다 —
 * 로그인 상태에서 현재 비밀번호를 아는 경우는 아래 {@code /password/change}를 쓴다.
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CurrentUser AuthenticatedUser me, @Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.of(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(authService.refresh(request.refreshToken()));
    }

    /**
     * 로그인 상태에서 현재 비밀번호를 확인하고 바로 새 비밀번호로 바꾼다. 비밀번호를 잊어버린 경우
     * (현재 비밀번호를 모르는 경우)는 관리자 강제 초기화(admin 전용
     * {@code POST /v1/users/{id}/reset-password})를 쓴다.
     */
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@CurrentUser AuthenticatedUser me, @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(me, request);
        return ApiResponse.of(null);
    }
}
