import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RSAUtil {

  public static PublicKey getPublicKey(String base64PublicKey) {
    PublicKey publicKey = null;
    try {
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey.getBytes()));
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      publicKey = keyFactory.generatePublic(keySpec);
      return publicKey;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      e.printStackTrace();
    }
    return publicKey;
  }

  public static PrivateKey getPrivateKey(String base64PrivateKey) {
    PrivateKey privateKey = null;
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey.getBytes()));
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    try {
      privateKey = keyFactory.generatePrivate(keySpec);
    } catch (InvalidKeySpecException e) {
      e.printStackTrace();
    }
    return privateKey;
  }

  public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
    Cipher encryptCipher = Cipher.getInstance("RSA");
    encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

    byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(UTF_8));

    return Base64.getEncoder().encodeToString(cipherText);
  }

  public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
    byte[] bytes = Base64.getDecoder().decode(cipherText);

    Cipher decriptCipher = Cipher.getInstance("RSA");
    decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

    return new String(decriptCipher.doFinal(bytes), UTF_8);
  }

  public static String publicKeyToString(PublicKey publicKey) {
    byte[] byte_pubkey = publicKey.getEncoded();
    String str_key = Base64.getEncoder().encodeToString(byte_pubkey);
    return str_key;
  }

  public static PublicKey getPemPublicKey(String fileKeyPath, String algorithm) throws Exception {
    File f = new File(fileKeyPath);
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream(fis);
    byte[] keyBytes = new byte[(int) f.length()];
    dis.readFully(keyBytes);
    dis.close();

    String temp = new String(keyBytes);
    String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
    publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");


    BASE64Decoder b64 = new BASE64Decoder();
    byte[] decoded = b64.decodeBuffer(publicKeyPEM);

    X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
    KeyFactory kf = KeyFactory.getInstance(algorithm);
    return kf.generatePublic(spec);
  }

  public static PrivateKey getPemPrivateKey(String fileKeyPath, String algorithm) throws Exception {
    File f = new File(fileKeyPath);
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream(fis);
    byte[] keyBytes = new byte[(int) f.length()];
    dis.readFully(keyBytes);
    dis.close();

    String stringPrivateKey = new String(keyBytes);

    stringPrivateKey = stringPrivateKey
      .replace("-----BEGIN PRIVATE KEY-----", "")
      .replace("-----END PRIVATE KEY-----", "")
      .replaceAll("\\s", "");

    // decode to get the binary DER representation
    byte[] privateKeyDER = Base64.getDecoder().decode(stringPrivateKey);

    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDER));
    return privateKey;
  }
}
