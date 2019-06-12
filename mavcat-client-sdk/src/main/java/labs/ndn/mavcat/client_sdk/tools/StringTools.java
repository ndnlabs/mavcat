package labs.ndn.mavcat.client_sdk.tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static java.lang.System.exit;

public class StringTools {
    public static String sha256(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            exit(-1);
        }
        return null;
    }

    // when parse failure, use default value
    public static int parsePositiveInteger(String str) {
        int val = 0;
        try {
            val =  Integer.parseInt(str);
        } catch (NumberFormatException e) {
            val = -1;
        }
        return val;
    }

    // when parse failure, use default value
    public static long parsePositiveLong(String str) {
        long val = 0;
        try {
            val =  Long.parseLong(str);
        } catch (NumberFormatException e) {
            val = -1;
        }
        return val;
    }

}
