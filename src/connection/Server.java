package connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
// import java.nio.CharBuffer;


import main.Core;
import exchange.EncryptedMessage;

public class Server implements Runnable, AutoCloseable {
  private ServerSocket serverSock;

  public Server() throws Exception {
    try {
      serverSock = new ServerSocket(Core.instance.getSettings().getPort());
    } catch (Exception e) {
      Core.instance.printError("[Server] Error in constructor. Not able to bind to port.\n"
          + e.getMessage(), e, true);
    }
  }

  @Override
  public void run() {

    while (true) {
      if (serverSock == null || Core.instance == null || !serverSock.isBound())
        throw new IllegalStateException("Unbound ServerSocket in Server class");
      if (Thread.interrupted() && serverSock != null) {
        try {
          serverSock.close();
        } catch (Throwable t) {
          System.err.println("[Server] Error while closing: " + t.getMessage());
        }
        return;
      }
      try (Socket clientSock = serverSock.accept();
          BufferedReader in =
              new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
          PrintWriter out = new PrintWriter(clientSock.getOutputStream());) {
        clientSock.setSoTimeout(Core.instance.getSettings().getConnectionTimeout());
        // CharBuffer input =
        // CharBuffer.allocate(Defaults.headerLenLimit+Defaults.msgLenLimit+2);
        // in.read(input);
        // String formattedMsg = input.toString();
        String formattedMsg = in.readLine();
        System.out.println("[Server] Received data: " + formattedMsg);
        try {
          EncryptedMessage msg = new EncryptedMessage(formattedMsg);
          Core.instance.getMessageManager().interpreteIncomingMessage(msg);
          out.write("true\n"); // Message was received and parsed
          // successfully
          out.flush();
        } catch (Exception e) {
          out.write("false\n"); // There was a parsing error
          out.flush();
          Core.instance.getUserInterface().printError(e);
        }
      } catch (Throwable t) {
        Core.instance.getUserInterface()
            .printError("[Server] Error while handling connection: " + t.getMessage());
      }
    }
  }

  /**
   * Tries to close the server. Once closed, the server cannot be started again.
   */
  @Override
  public void close() throws IOException {

    if (serverSock != null && !serverSock.isClosed())
      serverSock.close();

  }

  // private class SocketThread implements Runnable {
  // private NetworkTestReloaded Core.instance.instance.instance;
  // private Socket client;
  //
  // public SocketThread(Socket client, NetworkTestReloaded Core.instance.instance.instance) {
  // this.client = client;
  // Core.instance.instance.instance = Core.instance.instance.instance;
  // }
  //
  // @Override
  // public void run() {
  // try ( BufferedReader in = new BufferedReader(
  // new InputStreamReader(client.getInputStream()));
  // PrintWriter out = new PrintWriter(client.getOutputStream()); ) {
  //
  // CharBuffer input =
  // CharBuffer.allocate(Defaults.headerLenLimit+Defaults.msgLenLimit+2);
  // in.read(input);
  // String formattedMsg = input.toString();
  // System.out.println("[In SocketThread] Received data: "+formattedMsg);
  // try {
  // Message msg = new Message(formattedMsg);
  // Core.instance.instance.instance.addMessage(msg);
  // out.write("true"); //Message was received and parsed successfully
  // }
  // catch (Exception e) {
  // out.write("false"); //There was a parsing error
  // }
  //
  // }
  // catch (Exception e) {
  // e.printStackTrace(System.err);
  // }
  // }
  // }
}
