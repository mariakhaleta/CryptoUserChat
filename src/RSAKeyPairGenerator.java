import java.security.*;

public class RSAKeyPairGenerator {

  private PrivateKey privateKey;
  private PublicKey publicKey;

  public RSAKeyPairGenerator() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair pair = keyGen.generateKeyPair();
    this.privateKey = pair.getPrivate();
    this.publicKey = pair.getPublic();
  }

  PrivateKey getPrivateKey() {
    return privateKey;
  }

  PublicKey getPublicKey() {
    return publicKey;
  }
}
