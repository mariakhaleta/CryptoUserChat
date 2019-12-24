import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

class AESKeyGenerator {

  static String generateSecretKey() {
    KeyGenerator keyGen = null;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    assert keyGen != null;
    keyGen.init(256);
    SecretKey secretKey = keyGen.generateKey();
    return Base64.getEncoder().encodeToString(
      secretKey.getEncoded());
  }

  static SecretKey getSecretKey(String encodedKey) {
    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
  }

}
