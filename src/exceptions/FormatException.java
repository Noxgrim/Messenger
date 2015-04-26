package exceptions;

/**
 * A format exception. Will be thrown if a message is in the wrong format or a custom setting
 * requires an other format.
 */
public class FormatException extends Exception {

  private static final long serialVersionUID = 4545660648608033081L;

  public FormatException() {
    super();
  }

  public FormatException(String s) {
    super(s);
  }
}
