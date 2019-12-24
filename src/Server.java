import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
  private static int clientID;
  private ArrayList<ClientThread> clientsArrayList;
  private SimpleDateFormat simpleDateFormat;
  private int port;
  private boolean keepGoing;
  private String publicKeyServerFilePath = "server_keys/publickey.pem";
  private String privateKeyServerFilePath = "server_keys/privatekey-pkcs8.pem";
  private String algorithmCrypto = "RSA";
  private PublicKey publicKeyServer;
  private PrivateKey privateKeyServer;


  private Server(int port) {
    this.port = port;
    simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    clientsArrayList = new ArrayList<ClientThread>();
    try {
      publicKeyServer = RSAUtil.getPemPublicKey(publicKeyServerFilePath, algorithmCrypto);
      privateKeyServer = RSAUtil.getPemPrivateKey(privateKeyServerFilePath, algorithmCrypto);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void start() {
    keepGoing = true;
    try {
      ServerSocket serverSocket = new ServerSocket(port);
      while (keepGoing) {
        displayMessage("Server waiting for Clients on port " + port + ".");
        Socket socket = serverSocket.accept();
        if (!keepGoing)
          break;
        ClientThread t = new ClientThread(socket);
        clientsArrayList.add(t);
        t.start();
      }

      try {
        serverSocket.close();
        for (ClientThread tc : clientsArrayList) {
          try {
            tc.sInput.close();
            tc.sOutput.close();
            tc.socket.close();
          } catch (IOException ex) {
            displayMessage(ex.getMessage());
          }
        }
      } catch (Exception e) {
        displayMessage("Exception closing the server and clients: " + e);
      }
    } catch (IOException e) {
      String msg = simpleDateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
      displayMessage(msg);
    }
  }

  protected void stop() {
    keepGoing = false;
    try {
      new Socket("localhost", port);
    } catch (Exception e) {
      displayMessage(e.getMessage());
    }
  }

  private void displayMessage(String msg) {
    String time = simpleDateFormat.format(new Date()) + " " + msg;
    System.out.println(time);
  }

  private synchronized boolean sendMessage(String userSender, String message) {
    String time = simpleDateFormat.format(new Date());
    String[] messageToSplit = message.split(" ", 3);

    boolean isPrivate = false;
    if (messageToSplit[1].charAt(0) == '@')
      isPrivate = true;

    if (isPrivate) {
      String receiverName = messageToSplit[1].substring(1);
      message = messageToSplit[0] + messageToSplit[2];
      String messageToSend = time + " " + message + "\n";
      boolean found = false;

      for (int y = clientsArrayList.size(); --y >= 0; ) {
        ClientThread clientThread = clientsArrayList.get(y);
        String check = clientThread.getUsername();
        if (check.equals(receiverName)) {
          if (!clientThread.writeMsg(messageToSend)) {
            clientsArrayList.remove(y);
            displayMessage("Disconnected Client " + clientThread.username + " removed from list.");
          }
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    } else {
      String messageToSend = time + " " + message + "\n";
      System.out.print(messageToSend);

      for (int i = clientsArrayList.size(); --i >= 0; ) {
        ClientThread clientThread = clientsArrayList.get(i);
        if (!clientThread.writeMsg(messageToSend)) {
          clientsArrayList.remove(i);
          displayMessage("Disconnected Client " + clientThread.username + " removed from list.");
        }
      }
    }
    return true;
  }

  private void remove(int id) {
    for (int i = 0; i < clientsArrayList.size(); ++i) {
      ClientThread clientThread = clientsArrayList.get(i);
      if (clientThread.id == id) {
        clientsArrayList.remove(i);
        break;
      }
    }
  }

  public static void main(String[] args) {
    Server server = new Server(Constants.PORTNUMBER);
    server.start();
  }

  class ClientThread extends Thread {
    Socket socket;
    ObjectInputStream sInput;
    ObjectOutputStream sOutput;
    int id;
    String username;
    String userPublicKey;
    ChatMessage chatMessage;
    String date;

    ClientThread(Socket socket) {
      id = ++clientID;
      this.socket = socket;
      System.out.println("Thread trying to create Object Input/Output Streams");
      try {
        sOutput = new ObjectOutputStream(socket.getOutputStream());
        sInput = new ObjectInputStream(socket.getInputStream());
        username = (String) sInput.readObject();
        displayMessage(" *** " + username + " has joined the chat room." + " *** ");
      } catch (IOException | ClassNotFoundException e) {
        displayMessage("Exception creating new Input/output Streams: " + e);
        return;
      }
      date = new Date().toString() + "\n";
    }

    String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getUserPublicKey() {
      return userPublicKey;
    }

    public void run() {
      boolean keepGoing = true;
      try {
        ChatMessage chatMessage = (ChatMessage) sInput.readObject();
        String messageWithPublicKey = chatMessage.getMessage();
        userPublicKey = messageWithPublicKey;
        TextFileUtils.writeToJsonListClient(username, userPublicKey);
      } catch (Exception e) {
        e.printStackTrace();
      }

      while (keepGoing) {
        try {
          chatMessage = (ChatMessage) sInput.readObject();
        } catch (IOException ex) {
          displayMessage(username + " Exception reading Streams: " + ex);
          break;
        } catch (ClassNotFoundException ex2) {
          break;
        }
        String message = chatMessage.getMessage();

        switch (chatMessage.getType()) {
          case ChatMessage.CHATWITH:
            boolean confirmation = false;
            try {
              confirmation = sendMessageToClient(username, username + ": " + message);
            } catch (IOException | ClassNotFoundException e) {
              e.printStackTrace();
            }
            if (!confirmation) {
              String msg = " *** " + "Sorry. No such user exists." + " *** ";
              writeMsg(msg);
            }
            break;

          case ChatMessage.CLIENTSONLINE:
            writeMsg("List of the users connected at " + simpleDateFormat.format(new Date()) + "\n");
            for (int i = 0; i < clientsArrayList.size(); ++i) {
              ClientThread clientThread = clientsArrayList.get(i);
              writeMsg((i + 1) + ") " + clientThread.username + " since " + clientThread.date);
            }
            break;

          case ChatMessage.EXIT:
            displayMessage(username + " disconnected with a LOGOUT message.");
            keepGoing = false;
            break;

          case ChatMessage.SECRETMESSAGE:
            displayMessage(message);
        }
      }
      remove(id);
      close();
    }

    private synchronized boolean sendMessageToClient(String userSender, String message) throws IOException, ClassNotFoundException {
      String[] segments = message.split("@");
      String receiverName = segments[segments.length-1];
      boolean found = false;

      String encryptedMessage = null;
      for (int y = 0; y < clientsArrayList.size(); y++) {
        ClientThread clientThread = clientsArrayList.get(y);
        String checkUserName = clientThread.getUsername();
        if (checkUserName.equals(receiverName)) {
          y = 0;
          if(checkUserName.equals(userSender)) {
            clientThread.writeMsg("Public key receiver: " + clientThread.getUserPublicKey());
            ChatMessage chatMessage = (ChatMessage) sInput.readObject();
            encryptedMessage = chatMessage.getMessage();
          }
          break;
        }
      }

      for (ClientThread clientThread : clientsArrayList) {
        String checkUserName = clientThread.getUsername();
        if (checkUserName.equals(receiverName)) {
          clientThread.writeMsg("Encrypted message with session key: " + encryptedMessage);
          clientThread.writeMsg("Client @" + userSender + " start with you new conversation.");
        }
      }

      for (ClientThread clientThread : clientsArrayList) {
        String checkUserName = clientThread.getUsername();
        if (checkUserName.equals(userSender)) {
            ChatMessage chatMessage = (ChatMessage) sInput.readObject();
            String messageToSend = "Encrypted message: " + chatMessage.getMessage();
            clientThread.writeMsg(messageToSend);
        }
      }
      return found;
    }

    private void close() {
      try {
        if (sInput != null) sInput.close();
        if (sOutput != null) sOutput.close();
        if (socket != null) socket.close();
      } catch (Exception e) {
        displayMessage(e.getMessage());
      }
    }

    private boolean writeMsg(String msg) {
      if (!socket.isConnected()) {
        close();
        return false;
      }
      try {
        sOutput.writeObject(msg);
      } catch (IOException e) {
        displayMessage(" *** " + "Error sending message to " + username + " *** ");
        displayMessage(e.toString());
      }
      return true;
    }
  }
}

