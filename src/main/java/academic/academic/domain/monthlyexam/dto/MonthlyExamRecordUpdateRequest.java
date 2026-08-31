package academic.academic.domain.monthlyexam.dto;

public record MonthlyExamRecordUpdateRequest(
        Integer rawScore,
        Integer stdScore,
        Integer percentile,
        String grade
) {
}
