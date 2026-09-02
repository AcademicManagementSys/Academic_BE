package academic.academic.domain.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, UserSummary user) {
}
