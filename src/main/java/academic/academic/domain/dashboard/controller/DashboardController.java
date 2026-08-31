package academic.academic.domain.dashboard.controller;

import academic.academic.domain.dashboard.dto.ClassStatisticsResponse;
import academic.academic.domain.dashboard.dto.TeacherDashboardResponse;
import academic.academic.domain.dashboard.service.ClassStatisticsService;
import academic.academic.domain.dashboard.service.TeacherDashboardService;
import academic.academic.global.response.ApiResponse;
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

    @GetMapping("/teacher")
    public ApiResponse<TeacherDashboardResponse> getTeacherDashboard(@RequestParam Long teacherId,
                                                                       @RequestParam(required = false) String date) {
        return ApiResponse.of(teacherDashboardService.getTeacherDashboard(teacherId, date));
    }

    @GetMapping("/classes")
    public ApiResponse<List<ClassStatisticsResponse>> getClassStatistics(@RequestParam(required = false) String month) {
        return ApiResponse.of(classStatisticsService.getClassStatistics(month));
    }
}
