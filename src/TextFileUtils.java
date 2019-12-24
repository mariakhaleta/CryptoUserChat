import java.io.FileWriter;
import java.io.IOException;

public class TextFileUtils {

  public static void writeToJsonListClient(String clientName, String clientPublicKey) {
    try {
      FileWriter writer = new FileWriter("clients_list.txt", true);
      writer.write("clientName: " + clientName + " clientPublicKey: " + clientPublicKey);
      writer.write("\r\n");   // write new line
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
