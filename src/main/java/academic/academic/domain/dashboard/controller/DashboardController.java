package academic.academic.domain.dashboard.controller;

import academic.academic.domain.dashboard.dto.AdminDashboardResponse;
import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.dashboard.service.AdminDashboardService;
import academic.academic.domain.dashboard.service.ClassStatisticsService;
import academic.academic.domain.dashboard.service.TeacherDashboardService;
import academic.academic.domain.user.entity.Role;
import academic.academic.global.response.ApiResponse;
import academic.academic.global.security.AuthenticatedUser;
import academic.academic.global.security.AuthorizationService;
import academic.academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대시보드 API (SCR-02, SCR-03, FR-06-01 ~ FR-06-03)
 */
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TeacherDashboardService teacherDashboardService;
    private final ClassStatisticsService classStatisticsService;
    private final AdminDashboardService adminDashboardService;
    private final AuthorizationService authorizationService;

    @GetMapping("/admin")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard(@CurrentUser AuthenticatedUser me,
                                                                   @RequestParam(required = false) String date) {
        authorizationService.requireRole(me, Role.ADMIN);
        return ApiResponse.of(adminDashboardService.getAdminDashboard(date));
    }

    @GetMapping("/teacher")
    public ApiResponse<TeacherDashboardResponse> getTeacherDashboard(@CurrentUser AuthenticatedUser me,
                                                                       @RequestParam(required = false) String date) {
        authorizationService.requireRole(me, Role.TEACHER);
        return ApiResponse.of(teacherDashboardService.getTeacherDashboard(me.id(), date));
    }

    @GetMapping("/classes")
    public ApiResponse<List<ClassStatisticsResponse>> getClassStatistics(@CurrentUser AuthenticatedUser me,
                                                                           @RequestParam(required = false) String month) {
        authorizationService.requireRole(me, Role.ADMIN, Role.TEACHER);
        return ApiResponse.of(classStatisticsService.getClassStatistics(month));
    }
}
