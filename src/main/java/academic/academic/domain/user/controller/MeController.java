package academic.academic.domain.user.controller;

import academic.academic.domain.user.dto.UserResponse;
import academic.academic.domain.user.service.UserService;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /me — 로그인한 사용자 본인 정보 조회 (API_명세서_V2 §3).
 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/v1/me")
    public ApiResponse<UserResponse> getMe(@CurrentUser AuthenticatedUser me) {
        return ApiResponse.of(userService.getUser(me.id()));
    }
}
