package userInterface;

/**
 * Manages, how Errors and Exceptions are handled by the program.
 */
public interface ErrorManager {

  /**
   * Prints a custom error message on the screen.
   * 
   * @param shortMessage The message that will be printed.
   */
  public void printError(String shortMessage);

  /**
   * Prints a message and a StrackTrace at the screen.
   * 
   * @param shortMessage The message that will be printed.
   * @param t The Throwable that's StackTrace will be printed.
   */
  public void printError(String shortMessage, Throwable t);

  /**
   * Prints a StackTrace at the screen.
   * 
   * @param t Throwable that's StackTrace will be printed.
   */
  public void printError(Throwable t);

  /**
   * Prints a StackTrace the screen, optional labelled as fatal error.
   * 
   * @param t Throwable that's StackTrace will be printed.
   * @param fatal whether the Exception is a fatal error or not.
   */
  public void printError(Throwable t, boolean fatal);

  /**
   * Prints a message and a StackTrace the screen, optional labelled as fatal error.
   * 
   * @param shortMessage The message that will be printed.
   * @param t Throwable that's StackTrace will be printed.
   * @param fatal whether the Exception is a fatal error or not.
   */
  public void printError(String shortMessage, Throwable t, boolean fatal);

}
