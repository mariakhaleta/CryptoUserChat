import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;

public class TextFileUtils {

  public static void writeToJsonListClient(String clientName, String clientPublicKey) {
    ClientObjectGson clientObjectGson = new ClientObjectGson();
    clientObjectGson.setClientName(clientName);
    clientObjectGson.setClientPublicKey(clientPublicKey);

    Gson gson = new Gson();

    String json = gson.toJson(clientObjectGson);

    try (FileWriter writer = new FileWriter("clients_list.json")) {
      gson.toJson(json, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
