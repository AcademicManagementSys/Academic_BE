package academic.academic.domain.attendance.controller;

import academic.academic.domain.attendance.dto.AttendanceBulkRequest;
import academic.academic.domain.attendance.dto.AttendanceResponse;
import academic.academic.domain.attendance.dto.AttendanceUpdateRequest;
import academic.academic.domain.attendance.service.AttendanceService;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 출석 API (SCR-07, SCR-13, FR-02-01 ~ FR-02-06)
 */
@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/v1/attendance")
    public ApiResponse<List<AttendanceResponse>> getAttendance(@RequestParam Long classId,
                                                                 @RequestParam String date) {
        return ApiResponse.of(attendanceService.getAttendanceByClassAndDate(classId, date));
    }

    @PostMapping("/v1/attendance/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<AttendanceResponse>> saveBulk(@Valid @RequestBody AttendanceBulkRequest request) {
        return ApiResponse.of(attendanceService.saveBulk(request));
    }

    @PatchMapping("/v1/attendance/{id}")
    public ApiResponse<AttendanceResponse> updateAttendance(@PathVariable Long id,
                                                              @RequestBody AttendanceUpdateRequest request) {
        return ApiResponse.of(attendanceService.updateAttendance(id, request));
    }

    @GetMapping("/v1/students/{studentId}/attendance")
    public ApiResponse<List<AttendanceResponse>> getStudentAttendance(@PathVariable Long studentId,
                                                                        @RequestParam String month) {
        return ApiResponse.of(attendanceService.getStudentAttendance(studentId, month));
    }
}
