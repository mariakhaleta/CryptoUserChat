import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RSAUtil {

  static PublicKey getPublicKey(String base64PublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] data = Base64.decode(base64PublicKey);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
    KeyFactory fact = KeyFactory.getInstance("RSA");
    return fact.generatePublic(spec);
  }

  public static PrivateKey getPrivateKey(String base64PrivateKey) {
    PrivateKey privateKey = null;
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(base64PrivateKey));
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    try {
      assert keyFactory != null;
      privateKey = keyFactory.generatePrivate(keySpec);
    } catch (InvalidKeySpecException e) {
      e.printStackTrace();
    }
    return privateKey;
  }

  static String encrypt(String plainText, PublicKey publicKey) throws Exception {
    Cipher encryptCipher = Cipher.getInstance("RSA");
    encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

    byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(UTF_8));

    return Base64.encode(cipherText);
  }

  public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
    byte[] bytes = Base64.decode(cipherText);

    Cipher decriptCipher = Cipher.getInstance("RSA");
    decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

    return new String(decriptCipher.doFinal(bytes), UTF_8);
  }

  public static String publicKeyToString(PublicKey publicKey) {
    byte[] byte_pubkey = publicKey.getEncoded();
    String str_key = Base64.encode(byte_pubkey);
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

  static PrivateKey getPemPrivateKey(String fileKeyPath, String algorithm) throws Exception {
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
    byte[] privateKeyDER = Base64.decode(stringPrivateKey);

    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDER));
  }
}
