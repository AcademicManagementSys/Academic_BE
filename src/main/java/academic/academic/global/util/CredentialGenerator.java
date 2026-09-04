package academic.academic.global.util;

import java.security.SecureRandom;

/**
 * 계정 아이디 자동 생성 및 임시 비밀번호 발급용 (FR-01-07).
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
}
