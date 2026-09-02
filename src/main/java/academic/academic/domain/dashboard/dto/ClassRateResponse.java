package academic.academic.domain.dashboard.dto;

/**
 * 반별 오늘 출석률/숙제완료율 (0~1 스케일). API_명세서_V2 §13 GET /dashboard/teacher 참고.
 * 출석/숙제 기록이 하나도 없는 반은 rate가 null이다.
 */
public record ClassRateResponse(
        Long classId,
        String className,
        Double todayAttendanceRate,
        Double homeworkDoneRate
) {
}
