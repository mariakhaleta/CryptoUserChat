import javax.crypto.SecretKey;
import java.net.*;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class Client {
  private static ObjectInputStream sInput;    // to read from the socket
  private ObjectOutputStream sOutput;    // to write on the socket
  private Socket socket;          // socket object

  private String server, username;  // server and username
  private int port; // port
  private static PublicKey publicKey;
  private static PrivateKey privateKey;
  private static Client client = null;
  private SecretKey sessionKey = null;

  private Client(String server, int port, String username, String password) {
    this.server = server;
    this.port = port;
    this.username = username;
    try {
      String algorithmCrypto = "RSA";
      String publicKeyServerFilePath = "server_keys/publickey.pem";
      PublicKey publicKeyServer = RSAUtil.getPemPublicKey(publicKeyServerFilePath, algorithmCrypto);
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
      publicKey = keyPairGenerator.getPublicKey();
      privateKey = keyPairGenerator.getPrivateKey();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  private boolean start() {
    try {
      socket = new Socket(server, port);
    } catch (Exception ec) {
      displayMessage("Error connecting to server:" + ec);
      return false;
    }

    String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
    displayMessage(msg);

    try {
      sInput = new ObjectInputStream(socket.getInputStream());
      sOutput = new ObjectOutputStream(socket.getOutputStream());
    } catch (IOException eIO) {
      displayMessage("Exception creating new Input/output Streams: " + eIO);
      return false;
    }

    new ListenFromServer().start();
    try {
      sOutput.writeObject(username);
    } catch (IOException eIO) {
      displayMessage("Exception doing login : " + eIO);
      disconnect();
      return false;
    }
    return true;
  }

  private void displayMessage(String msg) {
    System.out.println(msg);
  }

  private void sendMessageToServer(ChatMessage message) {
    try {
      sOutput.writeObject(message);
    } catch (IOException e) {
      displayMessage("Exception writing to server: " + e);
    }
  }

  private void disconnect() {
    try {
      if (sInput != null) sInput.close();
      if (sOutput != null) sOutput.close();
      if (socket != null) socket.close();
    } catch (Exception e) {
      displayMessage(e.getMessage());
    }
  }

  public static void main(String args[]) {
    Client.createNewClient();
  }

  public static void createNewClient() {
    Scanner scan = new Scanner(System.in);
    System.out.println("Hello, type 'LOGIN' if you have account");
    System.out.println("or type 'SIGNUP' to create account ");
    String type = scan.nextLine();
    if (type.equalsIgnoreCase("LOGIN")) {
      //TODO
    } else if (type.equalsIgnoreCase("SIGNUP")) {
      signUpUser();
    } else {
      //TODO
    }
  }

  private static void signUpUser() {
    String userName = "User name";
    String userPassword = "Password";

    Scanner scan = new Scanner(System.in);
    System.out.println("Enter the username: ");
    userName = scan.nextLine();
    System.out.println("Enter the password: ");
    String password = scan.nextLine();
    userPassword = Utils.sha256(password + "." + Constants.SALT);

    client = new Client(Constants.SERVERADRESS, Constants.PORTNUMBER, userName, userPassword);
    if (!client.start())
      return;

    try {
      client.sendMessageToServer(new ChatMessage(ChatMessage.MESSAGE, RSAUtil.publicKeyToString(publicKey)));
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("\nHello!");
    System.out.println("Type 'CHATWITH <space> @username' to send private message to client");
    System.out.println("Type 'CLIENTSONLINE' to see who is online");
    System.out.println("Type 'EXIT' logoff from server");

    while (true) {
      String msg = scan.nextLine();
      if (msg.equalsIgnoreCase("EXIT")) {
        client.sendMessageToServer(new ChatMessage(ChatMessage.EXIT, ""));
        break;
      } else if (msg.equalsIgnoreCase("CLIENTSONLINE")) {
        client.sendMessageToServer(new ChatMessage(ChatMessage.CLIENTSONLINE, ""));
      } else {
        client.sendMessageToServer(new ChatMessage(ChatMessage.CHATWITH, msg));
      }
    }
    scan.close();
    client.disconnect();
  }

  private void generateSessionKey(String messageWithKey) {
    String[] segments = messageWithKey.split(":");
    String publicKeyStringReceiver = segments[segments.length - 1];
    PublicKey publicKeyReceiver = null;
    try {
      publicKeyReceiver = RSAUtil.getPublicKey(publicKeyStringReceiver);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      e.printStackTrace();
    }
    String sessionKey = AESKeyGenerator.generateSecretKey();
    try {
      String encryptByReceiver = RSAUtil.encrypt(sessionKey, publicKeyReceiver);
      client.sendMessageToServer(new ChatMessage(ChatMessage.MESSAGE, encryptByReceiver));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void saveSessionKey(String messageWithSessionKey) {
    String[] segments = messageWithSessionKey.split(":");
    String encryptedMessage = segments[segments.length - 1];
    System.out.println(encryptedMessage);
    String sessionKeyString = null;
    try {
      sessionKeyString = RSAUtil.decrypt(encryptedMessage, privateKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    sessionKey = AESKeyGenerator.getSecretKey(sessionKeyString);
  }

  private void startNewSecretChat() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Type your message: \n");
    String msg = scanner.nextLine();
    String stringEncryptedSessionKey = null;
    try {
      stringEncryptedSessionKey = AESUtil.encrypt(msg, sessionKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    client.sendMessageToServer(new ChatMessage(ChatMessage.SECRETMESSAGE, stringEncryptedSessionKey));
  }

  private void decryptMessage(String message) {
    try {
      String[] segments = message.split(":");
      String encryptedMessage = segments[segments.length - 1];
      System.out.println("encryptedMessage" + encryptedMessage);
      String finalMessage = AESUtil.decrypt(encryptedMessage, sessionKey);
      System.out.println(finalMessage);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  class ListenFromServer extends Thread {
    public void run() {
      while (true) {
        try {
          String msg = (String) sInput.readObject();
          if (msg.contains("Public key receiver")) {
            generateSessionKey(msg);
          } else if (msg.contains("Encrypted message with session key")) {
            saveSessionKey(msg);
          } else if (msg.contains("Client @")) {
            System.out.println(msg);
            startNewSecretChat();
          } else if (msg.contains("Encrypted message")) {
            decryptMessage(msg);
          } else {
            System.out.println(msg);
            System.out.print("> ");
          }
        } catch (IOException ex) {
          displayMessage(" *** " + "Server has closed the connection: " + ex + " *** ");
          break;
        } catch (ClassNotFoundException ex2) {
          displayMessage(ex2.getMessage());
        }
      }
    }
  }
}

