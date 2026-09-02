package academic.academic.domain.user.controller;

import academic.academic.domain.user.dto.UserResponse;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.service.UserService;
import academic.academic.global.security.JwtProvider;
import academic.academic.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@Import(JwtProvider.class)
class MeControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(2L, Role.TEACHER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void 내_정보를_조회한다() throws Exception {
        given(userService.getUser(2L)).willReturn(
                new UserResponse(2L, "김선생", Role.TEACHER, "teacher1", null, true, LocalDateTime.now()));

        mockMvc.perform(get("/v1/me").header("Authorization", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("teacher1"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
