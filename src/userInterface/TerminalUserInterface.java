package userInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import persons.Contact;
import main.Core;

public class TerminalUserInterface implements UserInterface {

  private Core parent;

  public TerminalUserInterface(Core parent) {
    this.parent = parent;

    if (System.getProperty("os.name").toLowerCase().contains("windows"))
      parent.getSettings().setColorShown(false);
  }

  @Override
  public void printError(Throwable t) {
    printError(null, t, false);

  }

  @Override
  public void printError(Throwable t, boolean fatal) {
    printError(null, t, fatal);

  }

  @Override
  public void printError(String shortMessage) {
    printError(shortMessage, null, false);

  }

  @Override
  public void printError(String shortMessage, Throwable t) {
    printError(shortMessage, t, false);

  }

  @Override
  public void printError(String shortMessage, Throwable t, boolean fatal) {
    PrintStream output = System.err;

    if (!fatal) {
      if (shortMessage != null)
        output.println(shortMessage);

      if (t != null && parent.getSettings().getPrintExceptions())
        t.printStackTrace(output);
    } else {
      output.print(TextColor.RED);
      if (shortMessage != null)
        output.println(shortMessage);

      if (t != null && parent.getSettings().getPrintExceptions())
        t.printStackTrace(output);
      output.print(TextColor.RESET);
      System.exit(42);

    }

  }

  @Override
  public void switchToCoversation(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addConversation(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeConverstaion(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printDebugMessage(String message) {
    if (parent.getSettings().getDebugMode())
      System.out.println(TextColor.GRAY + "<DEBUG>" + TextColor.LIGHT_GRAY + " " + message
          + TextColor.RESET);

  }

  @Override
  public void printSystemMessage(String message) {
    System.out.println(TextColor.GRAY + "<SYSTEM>" + TextColor.LIGHT_GRAY + " " + message.trim()
        + TextColor.RESET);
  }

  @Override
  public void printRawMessage(String message) {
    System.out.println(message);

  }

  @Override
  public void alertDialog(String message) {
    System.out.print(TextColor.BLUE + "<ALERT>" + TextColor.LIGHT_BLUE + " " + message.trim()
        + '\n' + TextColor.LIGHT_GRAY + "         (Press Enter to continue.)" + TextColor.RESET);
    try {
      new BufferedReader(new InputStreamReader(System.in)).readLine();
    } catch (IOException e) {
      this.printError(e);
    }

  }

  @Override
  public boolean confirmDialog(String message) {

    return this.confirmDialog(message, true);

  }

  @Override
  public boolean confirmDialog(String message, boolean defaultChoise) {
    System.out.print(TextColor.BLUE + "<CONFIRM>" + TextColor.LIGHT_BLUE + " " + message.trim()
        + ' ' + TextColor.LIGHT_GRAY + ((defaultChoise) ? "(Y/n)" : "(y/N)") + TextColor.LIGHT_BLUE
        + ": " + TextColor.RESET);
    try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
      String choise = r.readLine();
      return (choise.equalsIgnoreCase("y") || choise.equalsIgnoreCase("yes")) ? true
          : (choise.equalsIgnoreCase("n") || choise.equalsIgnoreCase("no")) ? false
              : defaultChoise;
    } catch (IOException e) {
      this.printError(e);
      return defaultChoise;
    }
  }

  @Override
  public String multipleOptionsDialog(String message, String... options) {
    // TODO Auto-generated method stub
    return null;
  }

}
