package userInterface;

/**
 * Manages how debug and system messages are managed by the program. It also provides a basic set of
 * user prompts.
 */
public interface MessageManager {

  /**
   * Prints a debug message. It only will be printed if the debug mode is enabled.
   * 
   * @param message The message to be printed.
   */
  public void printDebugMessage(String message);

  /**
   * Prints a system message. A system message is labelled {@code "<System>"}.
   * 
   * @param message The message that will be printed.
   */
  public void printSystemMessage(String message);

  /**
   * Prints a raw message without any formatting or prefixes.
   * 
   * @param message The message that will be printed.
   */
  public void printRawMessage(String message);

  /**
   * Shows an alert dialog.
   * 
   * @param message The text of the dialog
   */
  public void alertDialog(String message);

  /**
   * Shows a confirm dialog. A confirm dialog is a simple {@code yes|no} dialog.<br>
   * {@code yes} will be chosen if the user just presses {@code Enter} .
   * 
   * @param message The text of the dialog.
   * @return {@code true} if the user has chosen {@code yes}, else {@code false}.
   */
  public boolean confirmDialog(String message);

  /**
   * Shows a confirm dialog. A confirm dialog is a simple {@code yes|no} dialog.
   * 
   * @param message The text of the dialog.
   * @param defaultChoise chosen if the user just presses {@code Enter}.
   * @return {@code true} if the user has chosen {@code yes}, else {@code false}.
   */
  public boolean confirmDialog(String message, boolean defaultChoise);

  /**
   * Shows a dialog that gives the user multiple custom options to chose from.
   * 
   * @param message The text of the dialog
   * @param options The options of the dialog.
   * @return The options tahat has been selected by the user.
   */
  public String multipleOptionsDialog(String message, String... options);
}
