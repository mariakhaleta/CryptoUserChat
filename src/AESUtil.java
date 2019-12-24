import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

class AESUtil {

  public static String encrypt(String strToEncrypt, SecretKey secretKey)
  {
    try
    {
      byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
      IvParameterSpec ivspec = new IvParameterSpec(iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
      return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
    }
    catch (Exception e)
    {
      System.out.println("Error while encrypting: " + e.toString());
    }
    return null;
  }

  public static String decrypt(String strToDecrypt, SecretKey secretKey) {
    try
    {
      byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
      IvParameterSpec ivspec = new IvParameterSpec(iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
      return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    }
    catch (Exception e) {
      System.out.println("Error while decrypting: " + e.toString());
    }
    return null;
  }

  public static void main(String[] args)
  {
    String originalString = "howtodoinjava.com";
    SecretKey secretKey = AESKeyGenerator.getSecretKey(AESKeyGenerator.generateSecretKey());
    String encryptedString = AESUtil.encrypt(originalString, secretKey) ;
    String decryptedString = AESUtil.decrypt(encryptedString, secretKey) ;

    System.out.println(originalString);
    System.out.println(encryptedString);
    System.out.println(decryptedString);
  }

}
