package main;

import java.util.UUID;

import javax.activation.CommandInfo;
import javax.annotation.processing.Completion;

import persons.User;
import manager.ConversationManager;
import manager.MessageManager;
import misc.Settings;
import userInterface.GraphicalUserIterface;
import userInterface.TerminalUserInterface;
import userInterface.UserInterface;
import connection.Client;
import connection.Server;
import exchange.commands.Command;
import exchange.commands.ExitCommand;

/**
 * The Core class. <br>
 * Contains the {@code main} method and all important objects of the program.
 */
public class Core {
  /** An instance of this object. */
  private static Core instance;

  /** The settings of the program. */
  private Settings settings;

  /** The user interface of the program. */
  private UserInterface ui;

  /** The {@code MessageManager} of the program. */
  private MessageManager messageManager;
  /** The <code>ConversationManager</code> of the program. */
  private ConversationManager cm;
  
  /** The internal client. */
  private Client client;
  /** The internal server. */
  private Server server;
  /** The the data of the program's user. */
  private User user;

  /**
   * Constructs a new {@code Core} object.
   * 
   * @param arguments The command line arguments.
   */
  public Core(String[] arguments) {
    settings = new Settings(this);

    ui =
        (this.settings.getGuiMode()) ? new GraphicalUserIterface(this) : new TerminalUserInterface(
            this);

    messageManager = new MessageManager();

    client = new Client();

    Thread serverThread = null;
    try {
      Server testServer = null;
      try {
        server = new Server(this);
      } catch (Exception e) {
        ui.printError("Internal Server couldn't be started.", null, true);
      }
      serverThread = new Thread(testServer);
      serverThread.start();
    } catch (Throwable t) {
      ui.printError(null, t, true);
    }

  }

  /**
   * Runs all non constructor methods.
   */
  private void run() {

    if (ui instanceof TerminalUserInterface
        && System.getProperty("os.name").toLowerCase().contains("windows")) {
      settings.setColorShown(false);
      ui.printSystemMessage("Hey, you're running a Windows system, aren't you? "
          + "Sadly Windows' cmd.exe does not support ASCII color codes, so they are disabled.");
      settings.setColorShown(ui.confirmDialog("Do you want to reenable them?", false));
      ui.printSystemMessage("ASCII color codes re-enabled.");
      System.out.println(UUID.randomUUID().toString());
    }
      

  }

  /**
   * Gets the {@link Settings} of the program.
   */
  public Settings getSettings() {
    return settings;
  }

  /**
   * Gets the {@link UserInterface} of the program.
   */
  public UserInterface getUserInterface() {
    return ui;
  }

  /**
   * Gets the {@link MessageManager} of the program.
   */
  public MessageManager getMessageManager() {
    return messageManager;
  }

  /**
   * Gets the {@link ConversationManager} of the program.
   */
  public ConversationManager getConversationManager() {
    return cm;
  }

  /**
   * Gets the internal {@code Client}.
   */
  public Client getClient() {
    return client;
  }

  /**
   * Gets the internal {@code Server}.
   */
  public Server getServer() {
    return server;
  }
  
  /**
   * Get the data of the program's user.
   */
  public User getUser() {
    return user;
  }

  /**
   * Prints a message and a StackTrace the screen, optional labelled as fatal error.<br>
   * If no {@code UserInterface} is available the {@code System.err}- {@code PrintStream} will be
   * used.<br>
   * <b>Note:</b> The StackTrace is also printed if the {@code printExceptios} value of the
   * {@code Settings} object is set to {@code false}. <b>Also:</b> If the {@code UserInterface} is
   * not {@code null} it should be used instead.
   * 
   * @param shortMessage The message that will be printed. Can be {@code null}.
   * @param t Throwable that's StackTrace will be printed. Can be {@code null}.
   * @param fatal whether the Exception is a fatal error or not.
   */
  public void printError(String shortMessage, Throwable t, boolean fatal) {
    if (ui != null)
      ui.printError(shortMessage, t, fatal);
    else {
      if (!fatal) {
        if (shortMessage != null)
          System.err.println(shortMessage);

        if (t != null)
          t.printStackTrace(System.err);
      } else {
        System.err.println("A fatal error occurred:");
        if (shortMessage != null)
          System.err.println(shortMessage);

        if (t != null)
          t.printStackTrace(System.err);

        System.exit(42);

      }
    }
  }

  /**
   * The {@code main} method.<br>
   * Creates a new {@code Core} object.
   * 
   * @param args The arguments of the program.
   */
  public static void main(String[] args) {
    instance = new Core(args);
    instance.run();
  }

  /**
   * Gets an instance of this object.
   */
  public static Core getInstance() {
    return instance;
  }
}
