package academic.academic.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Refresh Token / 비밀번호 재설정 토큰처럼 자체로 이미 고엔트로피인 opaque 값을 DB에 저장하기 위한
 * 단방향 해시. 비밀번호가 아니므로(브루트포스 대상 키 공간이 넓음) bcrypt 대신 빠른 SHA-256을 쓴다.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
