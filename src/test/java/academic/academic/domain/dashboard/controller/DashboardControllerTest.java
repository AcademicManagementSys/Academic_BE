package academic.academic.domain.dashboard.controller;

import academic.academic.domain.dashboard.dto.AdminDashboardResponse;
import academic.academic.domain.dashboard.dto.ClassRateResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.dashboard.service.AdminDashboardService;
import academic.academic.domain.dashboard.service.ClassStatisticsService;
import academic.academic.domain.dashboard.service.TeacherDashboardService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.JwtProvider;
import academic.academic.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(JwtProvider.class)
class DashboardControllerTest {

    private static final String TEACHER_TOKEN = AuthTestSupport.bearer(1L, Role.TEACHER);
    private static final String ADMIN_TOKEN = AuthTestSupport.bearer(9L, Role.ADMIN);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherDashboardService teacherDashboardService;

    @MockitoBean
    private ClassStatisticsService classStatisticsService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void 선생님_대시보드를_조회한다() throws Exception {
        given(teacherDashboardService.getTeacherDashboard(eq(1L), eq("2026-08-19")))
                .willReturn(new TeacherDashboardResponse(List.of(
                        new ClassRateResponse(3L, "중2 심화반", 0.95, 0.88)
                ), List.of(
                        new ClassRateResponse(1L, "초등 문법반", 0.9, 0.8),
                        new ClassRateResponse(3L, "중2 심화반", 0.95, 0.88)
                )));

        mockMvc.perform(get("/v1/dashboard/teacher").header("Authorization", TEACHER_TOKEN).param("date", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myClasses[0].className").value("중2 심화반"))
                .andExpect(jsonPath("$.data.myClasses[0].todayAttendanceRate").value(0.95))
                .andExpect(jsonPath("$.data.myClasses[0].homeworkDoneRate").value(0.88))
                .andExpect(jsonPath("$.data.allClassesSummary[0].className").value("초등 문법반"));
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/v1/dashboard/teacher"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 선생님이_아니면_403을_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN_ROLE, "이 API에 접근할 수 있는 역할이 아닙니다."))
                .given(authorizationService).requireRole(any(), eq(Role.TEACHER));

        mockMvc.perform(get("/v1/dashboard/teacher").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_ROLE"));
    }

    @Test
    void 관리자_대시보드를_조회한다() throws Exception {
        given(adminDashboardService.getAdminDashboard(eq("2026-08-19")))
                .willReturn(new AdminDashboardResponse(LocalDate.of(2026, 8, 19), 5, 80.0, 2, List.of(
                        new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)
                )));

        mockMvc.perform(get("/v1/dashboard/admin").header("Authorization", ADMIN_TOKEN).param("date", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudentCount").value(5))
                .andExpect(jsonPath("$.data.todayAttendanceRate").value(80.0))
                .andExpect(jsonPath("$.data.todayHomeworkUncheckedCount").value(2))
                .andExpect(jsonPath("$.data.classes[0].className").value("중2 심화반"));
    }

    @Test
    void 관리자_대시보드_date_형식이_올바르지_않으면_422를_반환한다() throws Exception {
        given(adminDashboardService.getAdminDashboard(eq("2026/08/19")))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "date 형식이 올바르지 않습니다. (yyyy-MM-dd)"));

        mockMvc.perform(get("/v1/dashboard/admin").header("Authorization", ADMIN_TOKEN).param("date", "2026/08/19"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 반별_통계를_조회한다() throws Exception {
        given(classStatisticsService.getClassStatistics(eq("2026-08")))
                .willReturn(List.of(new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)));

        mockMvc.perform(get("/v1/dashboard/classes").header("Authorization", TEACHER_TOKEN).param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attendanceRate").value(66.7))
                .andExpect(jsonPath("$.data[0].homeworkCompletionRate").value(75.0));
    }
}
