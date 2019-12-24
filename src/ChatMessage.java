import java.io.*;

class ChatMessage implements Serializable {

  static final int CLIENTSONLINE = 0, MESSAGE = 1, EXIT = 2, CHATWITH = 3, SECRETMESSAGE = 4;
  private int type;
  private String message;

  ChatMessage(int type, String message) {
    this.type = type;
    this.message = message;
  }

  int getType() {
    return type;
  }

  String getMessage() {
    return message;
  }
}
