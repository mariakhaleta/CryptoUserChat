import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

class Utils {
  static String sha256(String unCodedString) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(unCodedString.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();

      for (byte aHash : hash) {
        String hex = Integer.toHexString(0xff & aHash);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
