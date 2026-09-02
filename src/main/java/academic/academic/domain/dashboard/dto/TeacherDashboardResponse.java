package academic.academic.domain.dashboard.dto;

import java.util.List;

/**
 * API_명세서_V2 §13 GET /dashboard/teacher 응답 형태 (v1.1).
 * myClasses: 담당 반, allClassesSummary: 학원 전체 반 요약 — 둘 다 같은 날짜의 출석률/숙제완료율.
 */
public record TeacherDashboardResponse(
        List<ClassRateResponse> myClasses,
        List<ClassRateResponse> allClassesSummary
) {
}
