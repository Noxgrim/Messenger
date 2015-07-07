package exceptions;

public class UnknownUuidException extends Exception {

  private static final long serialVersionUID = -5915538207681745794L;
  
  public UnknownUuidException() {
    super();
  }
  
  public UnknownUuidException(String message) {
    super(message);
  }

}
