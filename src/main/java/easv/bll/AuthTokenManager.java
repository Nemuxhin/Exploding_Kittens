package easv.bll;

import easv.be.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * This class creates a small JWT token after a successful login.
 * It uses only standard Java classes, so the code stays easy to study.
 */
public class AuthTokenManager {

    private static final String SECRET = "exam-project-demo-secret";

    public String createToken(User user) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + safeJson(user.getUsername()) + "\",\"role\":\""
                + safeJson(user.getRole()) + "\",\"iat\":" + Instant.now().getEpochSecond() + "}";

        String header = base64Url(headerJson);
        String payload = base64Url(payloadJson);
        String signature = sign(header + "." + payload);

        return header + "." + payload + "." + signature;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("The authentication token could not be created.", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String safeJson(String value) {
        return value == null ? "" : value.replace("\"", "");
    }
}
