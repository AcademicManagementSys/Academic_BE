package academic.academic.domain.parentstudent.controller;

import academic.academic.domain.parentstudent.dto.ChildResponse;
import academic.academic.domain.parentstudent.dto.ParentStudentResponse;
import academic.academic.domain.parentstudent.entity.RelationType;
import academic.academic.domain.parentstudent.service.ParentStudentService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.JwtProvider;
import academic.academic.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentStudentController.class)
@Import(JwtProvider.class)
class ParentStudentControllerTest {

    private static final String PARENT_TOKEN = AuthTestSupport.bearer(45L, Role.PARENT);
    private static final String ADMIN_TOKEN = AuthTestSupport.bearer(1L, Role.ADMIN);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParentStudentService parentStudentService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 자녀_목록을_조회한다() throws Exception {
        given(parentStudentService.getChildren(45L)).willReturn(List.of(
                new ChildResponse(101L, "김민준", "중2", "중2 심화반", RelationType.MOTHER)
        ));

        mockMvc.perform(get("/v1/me/children").header("Authorization", PARENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].relationType").value("mother"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/me/children"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 학부모_자녀_연결을_생성한다() throws Exception {
        given(parentStudentService.createLink(any())).willReturn(
                new ParentStudentResponse(1L, 45L, "김민준 학부모", 101L, "김민준", RelationType.MOTHER));

        mockMvc.perform(post("/v1/parent-links").header("Authorization", ADMIN_TOKEN).contentType("application/json")
                        .content("{\"parentUserId\":45,\"studentId\":101,\"relationType\":\"mother\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.relationType").value("mother"));
    }

    @Test
    void 부모_구분을_수정한다() throws Exception {
        given(parentStudentService.updateLink(eq(1L), any())).willReturn(
                new ParentStudentResponse(1L, 45L, "김민준 학부모", 101L, "김민준", RelationType.FATHER));

        mockMvc.perform(patch("/v1/parent-links/1").header("Authorization", ADMIN_TOKEN).contentType("application/json")
                        .content("{\"relationType\":\"father\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relationType").value("father"));
    }

    @Test
    void 연결을_해제한다() throws Exception {
        mockMvc.perform(delete("/v1/parent-links/1").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isNoContent());
    }
}
