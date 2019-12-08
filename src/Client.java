import java.net.*;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

public class Client {
  private ObjectInputStream sInput;    // to read from the socket
  private ObjectOutputStream sOutput;    // to write on the socket
  private Socket socket;          // socket object

  private String server, username;  // server and username
  private String password; // password
  private int port; // port
  private PrivateKey privateKey;
  private static PublicKey publicKey;
  private static PublicKey publicKeyServer;
  private String publicKeyServerFilePath = "server_keys/publickey.pem";
  private String algorithmCrypto = "RSA";

  private Client(String server, int port, String username, String password) {
    this.server = server;
    this.port = port;
    this.username = username;
    this.password = password;
    try {
      publicKeyServer = RSAUtil.getPemPublicKey(publicKeyServerFilePath, algorithmCrypto);
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

  public static void main(String[] args) {
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

    Client client = new Client(Constants.SERVERADRESS, Constants.PORTNUMBER, userName, userPassword);
    if (!client.start())
      return;

    try {
      String encodedClientPublicKey = RSAUtil.encrypt(RSAUtil.publicKeyToString(publicKey), publicKeyServer);
      client.sendMessageToServer(new ChatMessage(ChatMessage.MESSAGE, encodedClientPublicKey));
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("\nHello!");
    System.out.println("Type '@username<space>yourmessage' to send message to client");
    System.out.println("Type 'CLIENTSONLINE' to see who is online");
    System.out.println("Type 'exit' logoff from server");

    while (true) {
      String msg = scan.nextLine();
      if (msg.equalsIgnoreCase("LOGOUT")) {
        client.sendMessageToServer(new ChatMessage(ChatMessage.exit, ""));
        break;
      } else if (msg.equalsIgnoreCase("WHOISIN")) {
        client.sendMessageToServer(new ChatMessage(ChatMessage.CLIENTSONLINE, ""));
      } else {
        client.sendMessageToServer(new ChatMessage(ChatMessage.MESSAGE, msg));
      }
    }
    scan.close();
    client.disconnect();
  }

  class ListenFromServer extends Thread {
    public void run() {
      while (true) {
        try {
          String msg = (String) sInput.readObject();
          System.out.println(msg);
          System.out.print("> ");
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

