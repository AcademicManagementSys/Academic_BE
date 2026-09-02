package academic.academic.domain.auth.service;

import academic.academic.domain.auth.dto.LoginRequest;
import academic.academic.domain.auth.dto.LoginResponse;
import academic.academic.domain.auth.dto.PasswordResetConfirmRequest;
import academic.academic.domain.auth.dto.PasswordResetRequestResponse;
import academic.academic.domain.auth.dto.TokenPairResponse;
import academic.academic.domain.auth.dto.UserSummary;
import academic.academic.domain.auth.entity.PasswordResetToken;
import academic.academic.domain.auth.entity.RefreshToken;
import academic.academic.domain.auth.repository.PasswordResetTokenRepository;
import academic.academic.domain.auth.repository.RefreshTokenRepository;
import academic.academic.domain.parentstudent.repository.ParentStudentRepository;
import academic.academic.domain.user.entity.Role;
import academic.academic.domain.user.entity.User;
import academic.academic.domain.user.repository.UserRepository;
import academic.academic.global.exception.BusinessException;
import academic.academic.global.exception.ErrorCode;
import academic.academic.global.security.JwtProvider;
import academic.academic.global.util.CredentialGenerator;
import academic.academic.global.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 로그인/토큰 재발급/로그아웃/비밀번호 자가 재설정 (API_명세서_V2 §3, REQ-AUTH-01~07).
 * 비밀번호 재설정 이메일 발송은 인프라가 없어 생략하고 토큰 발급까지만 구현한다 — 대신 관리자 초기화
 * ({@link academic.academic.domain.user.service.UserService#resetPassword})가 REQ-AUTH-07의 대안 경로를
 * 이미 충족한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "비활성화된 계정입니다.");
        }

        TokenPairResponse tokens = issueTokenPair(user);
        boolean hasMultipleChildren = user.getRole() == Role.PARENT
                && parentStudentRepository.findByParentUserId(user.getId()).size() > 1;
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), UserSummary.of(user, hasMultipleChildren));
    }

    @Transactional
    public void logout(String refreshToken) {
        jwtProvider.parseRefreshToken(refreshToken)
                .ifPresent(parsed -> refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshToken))
                        .ifPresent(RefreshToken::revoke));
    }

    @Transactional
    public TokenPairResponse refresh(String refreshToken) {
        JwtProvider.ParsedToken parsed = jwtProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "토큰이 유효하지 않거나 만료되었습니다."));

        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "폐기되었거나 존재하지 않는 토큰입니다."));
        if (!stored.isUsable(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "만료되었거나 이미 사용된 토큰입니다.");
        }
        stored.revoke();

        User user = userRepository.findById(parsed.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "비활성화된 계정입니다.");
        }
        return issueTokenPair(user);
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아이디를 찾을 수 없습니다. loginId=" + loginId));

        String rawToken = CredentialGenerator.randomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(PASSWORD_RESET_TOKEN_TTL);
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(rawToken))
                .expiresAt(expiresAt)
                .build());
        return new PasswordResetRequestResponse(rawToken, expiresAt);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.resetToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED, "토큰이 유효하지 않습니다."));
        if (!token.isUsable(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "만료되었거나 이미 사용된 토큰입니다.");
        }
        token.markUsed();
        token.getUser().changePassword(passwordEncoder.encode(request.newPassword()));
    }

    private TokenPairResponse issueTokenPair(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole(), user.getLoginId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole(), user.getLoginId());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(refreshToken))
                .expiresAt(LocalDateTime.now().plus(jwtProvider.getRefreshTokenTtl()))
                .build());
        return new TokenPairResponse(accessToken, refreshToken);
    }
}
