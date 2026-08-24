package academic.academic.domain.schoolclass.controller;

import academic.academic.domain.schoolclass.dto.ClassCreateRequest;
import academic.academic.domain.schoolclass.dto.ClassResponse;
import academic.academic.domain.schoolclass.dto.ClassUpdateRequest;
import academic.academic.domain.schoolclass.service.SchoolClassService;
import academic.academic.domain.student.dto.StudentResponse;
import academic.academic.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 반 관리 API (SCR-05, FR-01-02)
 */
@RestController
@RequestMapping("/v1/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @GetMapping
    public ApiResponse<List<ClassResponse>> getClasses(@RequestParam(required = false) Long teacherId) {
        return ApiResponse.of(schoolClassService.getClasses(teacherId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassResponse> createClass(@Valid @RequestBody ClassCreateRequest request) {
        return ApiResponse.of(schoolClassService.createClass(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassResponse> getClass(@PathVariable Long id) {
        return ApiResponse.of(schoolClassService.getClass(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ClassResponse> updateClass(@PathVariable Long id, @RequestBody ClassUpdateRequest request) {
        return ApiResponse.of(schoolClassService.updateClass(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable Long id) {
        schoolClassService.deleteClass(id);
    }

    @GetMapping("/{id}/students")
    public ApiResponse<List<StudentResponse>> getClassStudents(@PathVariable Long id) {
        return ApiResponse.of(schoolClassService.getClassStudents(id));
    }
}
