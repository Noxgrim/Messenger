package connection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import exchange.InternalMessage;
import main.Core;

public class Client {
  public boolean sendMessage(InternalMessage msg, InetSocketAddress address) {
    if (address.isUnresolved())
      return false;
    try (Socket sock = new Socket(address.getAddress(), address.getPort());) {
      sock.setSoTimeout(Core.getInstance().getSettings().getConnectionTimeout());

      if (!sendData(msg.getFormatted(), sock))
        return false;

      String parsingFeedback = readLine(sock);
      if (parsingFeedback != null && Boolean.parseBoolean(parsingFeedback)) {
        return true;
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  private boolean sendData(String data, Socket sock) {
    try (PrintWriter out = new PrintWriter(sock.getOutputStream());) {
      if (!data.endsWith("\n"))
        data += '\n';
      out.print(data);
      out.flush();
    } catch (Throwable t) {
      return false;
    }
    return true;
  }

  private String readLine(Socket sock) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));) {
      return in.readLine();
    } catch (Throwable t) {
      return null;
    }
  }
}
