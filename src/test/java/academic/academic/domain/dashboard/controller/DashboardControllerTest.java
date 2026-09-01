package academic.academic.domain.dashboard.controller;

import academic.academic.domain.dashboard.dto.AdminDashboardResponse;
import academic.academic.domain.dashboard.dto.ClassChecklistResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.dashboard.service.AdminDashboardService;
import academic.academic.domain.dashboard.service.ClassStatisticsService;
import academic.academic.domain.dashboard.service.TeacherDashboardService;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherDashboardService teacherDashboardService;

    @MockitoBean
    private ClassStatisticsService classStatisticsService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    void 선생님_대시보드를_조회한다() throws Exception {
        given(teacherDashboardService.getTeacherDashboard(eq(1L), eq("2026-08-19")))
                .willReturn(new TeacherDashboardResponse(LocalDate.of(2026, 8, 19), List.of(
                        new ClassChecklistResponse(3L, "중2 심화반", 2, 1, 1, 1, 1, 1)
                ), List.of(
                        new ClassStatisticsResponse(1L, "초등 문법반", 2L, "이선생", 3, 90.0, 80.0)
                )));

        mockMvc.perform(get("/v1/dashboard/teacher").param("teacherId", "1").param("date", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classes[0].className").value("중2 심화반"))
                .andExpect(jsonPath("$.data.classes[0].attendanceUncheckedCount").value(1))
                .andExpect(jsonPath("$.data.allClassesSummary[0].className").value("초등 문법반"));
    }

    @Test
    void 관리자_대시보드를_조회한다() throws Exception {
        given(adminDashboardService.getAdminDashboard(eq("2026-08-19")))
                .willReturn(new AdminDashboardResponse(LocalDate.of(2026, 8, 19), 5, 80.0, 2, List.of(
                        new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)
                )));

        mockMvc.perform(get("/v1/dashboard/admin").param("date", "2026-08-19"))
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

        mockMvc.perform(get("/v1/dashboard/admin").param("date", "2026/08/19"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 선생님이_아니면_422를_반환한다() throws Exception {
        given(teacherDashboardService.getTeacherDashboard(eq(2L), isNull()))
                .willThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "선택한 사용자는 선생님이 아닙니다."));

        mockMvc.perform(get("/v1/dashboard/teacher").param("teacherId", "2"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 반별_통계를_조회한다() throws Exception {
        given(classStatisticsService.getClassStatistics(eq("2026-08")))
                .willReturn(List.of(new ClassStatisticsResponse(3L, "중2 심화반", 1L, "김선생", 2, 66.7, 75.0)));

        mockMvc.perform(get("/v1/dashboard/classes").param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attendanceRate").value(66.7))
                .andExpect(jsonPath("$.data[0].homeworkCompletionRate").value(75.0));
    }
}
