package academic.academic.global.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 계정 아이디 자동 생성 및 임시 비밀번호 발급용 (FR-01-07), 비밀번호 재설정 토큰 발급용 (REQ-AUTH-07).
 */
public final class CredentialGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LOGIN_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";

    private CredentialGenerator() {
    }

    public static String randomLoginId(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(LOGIN_ID_CHARS.charAt(RANDOM.nextInt(LOGIN_ID_CHARS.length())));
        }
        return sb.toString();
    }

    public static String randomPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** 32바이트 난수를 URL-safe Base64로 인코딩한 고엔트로피 opaque 토큰 (비밀번호 재설정용). */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
