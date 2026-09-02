package academic.academic.domain.auth.controller;

import academic.academic.domain.auth.dto.LoginRequest;
import academic.academic.domain.auth.dto.LoginResponse;
import academic.academic.domain.auth.dto.PasswordResetConfirmRequest;
import academic.academic.domain.auth.dto.PasswordResetRequestRequest;
import academic.academic.domain.auth.dto.PasswordResetRequestResponse;
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
 * 인증 API (API_명세서_V2 §3, REQ-AUTH-01·06·07). {@code /v1/me}(현재 사용자 조회)는
 * {@link academic.academic.domain.user.controller.MeController}에 있다.
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

    @PostMapping("/password/reset-request")
    public ApiResponse<PasswordResetRequestResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        return ApiResponse.of(authService.requestPasswordReset(request.loginId()));
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ApiResponse.of(null);
    }
}
