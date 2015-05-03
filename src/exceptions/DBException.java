package exceptions;

public class DBException extends Exception {
  private static final long serialVersionUID = -6911684298687765328L;
  public DBException() {
    super();
  }
  public DBException(String message) {
    super(message);
  }
  public DBException(String string, Exception e) {
    super(string+e.getMessage());
  }
}
