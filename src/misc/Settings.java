package misc;

import java.io.File;

/**
 * A Settings object that will contain the settings of the program.<br>
 * Every value can be changed via the command line.<br>
 * <br>
 * 
 * A Settings object contains the following values: <br>
 * <ul>
 * <li>its save location
 * <li>the save location of the database
 * <li>the delimiter (final)<br>
 * <li>the message length limit<br>
 * <li>the header length limit<br>
 * <li>the nickname length limit<br>
 * <li>the connection timeout<br>
 * <li>the session key length<br>
 * <li>the public and private key<br>
 * <li>the active UI<br>
 * <li>the 'debug mode'-boolean<br>
 * <li>the user's nickname<br>
 * <li>the 'print exceptions'-boolean
 * </ul>
 */
public class Settings extends ConfiguarionFile {

  /**
   * The character that separates the different sections of a Message from each other.<br>
   * (<code>U+001D</code>, 'Group separator'-character)
   */
  private final char delimiter = '\u001D';

  /** The message length limit in characters. */
  @Data(defaultValue = "4096")
  private int msgLenLimit = 4096;
  /** The header length limit in characters. */
  @Data(defaultValue = "256")
  private int headerLenLimit = 256;
  /** The nickname length limit in characters. */
  @Data(defaultValue = "64") 
  private int nickLenLimit = 64;
  /** The length of the generated session key in bits. */
  @Data(defaultValue = "128")
  private int sessionKeyLen = 128;
  /** The socket timeout time in milliseconds. */
  @Data(defaultValue = "1000")
  private int connectionTimeout = 1000;

  /**
   * The boolean that determines if the program starts in terminal or GUI mode.
   */
  @Data(defaultValue = "false", getter = "getGuiMode", setter = "setGuiMode")
  private boolean gui = false;
  /** The boolean that determines if exceptions will be printed. */
  @Data(defaultValue = "false", getter = "getPrintExceptions", setter = "setPrintExceptions")
  private boolean exceptions = false;
  /**
   * The boolean that determines if debug messages will be printed. Can be set via Command line
   * arguments.
   */
  @Data(defaultValue = "false", save = false, load = false)
  private boolean debug = false;
  /** The boolean that determines if colors will be shown. */
  @Data(defaultValue = "true", getter = "getColorShown", setter = "setColorShown")
  private boolean color = true;
  
  /**
   * The port of the internal server.
   */
  @Data(defaultValue = "1337")
  private int port = 1337;

  /** Path to the SQLite database file. */
  @Data(defaultValue = "./data/messengerDB.sqlite", filePath = true)
  private String dbLocation;

  /**
   * Constructs a new {@code Settings } object and loads its values from the {@code messenger.conf}
   * file.
   */
  public Settings() {
    super("load", "." + File.separatorChar + "data" + File.separatorChar + "messenger.conf");
  }

  /**
   * Constructs a new {@code Settings} object.<br>
   * 
   * @see ConfiguarionFile#ConfiguarionFile(String, String)
   */
  public Settings(String creationType, String fileLocation) {
    super(creationType, fileLocation);

  }

  /**
   * Gets the message length limit in characters.
   * 
   * @return The message length limit.
   */
  public int getMsgLenLimit() {
    return msgLenLimit;
  }

  /**
   * Sets the message length limit.<br>
   * The minimum value is {@code 1} and the default value is {@code 4096}.
   * 
   * @param msgLenLimit The message length limit to be set.
   */
  public void setMsgLenLimit(int msgLenLimit) {
    this.msgLenLimit = this.validateInt(msgLenLimit, 1, Integer.MAX_VALUE, this.msgLenLimit);
  }

  /**
   * Gets the header length limit is characters.
   * 
   * @return The header length limit.
   */
  public int getHeaderLenLimit() {
    return headerLenLimit;
  }

  /**
   * Sets the header length limit in characters.<br>
   * The minimum value is {@code 64} and the default value is {@code 256}.
   * 
   * @param headerLenLimit The header length limit to be set.
   */
  public void setHeaderLenLimit(int headerLenLimit) {
    this.headerLenLimit =
        this.validateInt(headerLenLimit, 64, Integer.MAX_VALUE, this.headerLenLimit);
  }

  /**
   * Gets the nickname length limit in characters.
   * 
   * @return The nickname length limit.
   */
  public int getNickLenLimit() {
    return nickLenLimit;
  }

  /**
   * Sets the nickname length limit.<br>
   * The minimum value is {@code 1} and the default value is {@code 64}.
   * 
   * @param headerLenLimit The nickname length limit to be set.
   */
  public void setNickLenLimit(int nickLenLimit) {
    this.nickLenLimit = this.validateInt(nickLenLimit, 1, Integer.MAX_VALUE, this.nickLenLimit);
  }

  /**
   * Gets the session key length in characters.
   * 
   * @return The session key length.
   */
  public int getSessionKeyLen() {
    return sessionKeyLen;
  }

  /**
   * Sets the session key length limit.<br>
   * The minimum value is {@code 8} and the default value is {@code 128}.
   * 
   * @param sessionKeyLen The session key length to be set.
   */
  public void setSessionKeyLen(int sessionKeyLen) {
    this.sessionKeyLen = this.validateInt(sessionKeyLen, 128, 128, 192, 256);
  }

  /**
   * Sets the socket timeout in milliseconds.
   */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Sets the socket timeout to a specific value.
   * 
   * @param conectionTimeout the timeout in milliseconds. <br>
   *        The minimum timeout time is {@code 1000} milliseconds (1 second).
   */
  public void setConnectionTimeout(int conectionTimeout) {
    this.connectionTimeout =
        this.validateInt(conectionTimeout, 1000, Integer.MAX_VALUE, this.connectionTimeout);
  }

  /**
   * Get the mode of the user interface.
   * 
   * @return {@code true} if the user interface is in 'GUI' mode and {@code false} if the user
   *         interface is in 'terminal' mode.
   */
  public boolean getGuiMode() {
    return gui;
  }

  /**
   * Sets the mode of the user interface.<br>
   * While {@code true} means the user interface is in 'GUI' mode and {@code false} means the user
   * interface is in 'terminal' mode.
   * 
   * @param guiMode the user interface mode to be set.
   */
  public void setGuiMode(boolean guiMode) {
    this.gui = guiMode;
  }

  /**
   * Gets if {@code Exceptions} are printed or not.
   * 
   * @return if {@code Exceptions} are printed or not.
   */
  public boolean getPrintExceptions() {
    return exceptions;
  }

  /**
   * Set if {@code Exceptions} are printed or not.
   * 
   * @param exceptions if {@code Exceptions} are printed or not.
   */
  public void setPrintExceptions(boolean printExceptions) {
    this.exceptions = printExceptions;
  }

  /**
   * Gets if the program runs in debug mode or not.<br>
   * Debug messages only will displayed in this mode.
   * 
   * @return If the program runs in debug mode.
   */
  public boolean getDebugMode() {
    return debug;
  }

  /**
   * Sets if the program runs in debug mode or not.
   * 
   * @param debug if the program runs in debug mode or not.
   */
  public void setDebugMode(boolean debug) {
    this.debug = debug;
  }

  /**
   * Gets if colors will be shown in the program.
   */
  public boolean getColorShown() {
    return color;
  }

  /**
   * Sets if colors will be shown in the program.
   * 
   * @param color If colors will be shown.
   */
  public void setColorShown(boolean color) {
    this.color = color;
  }

  /**
   * Gets the port the internal server will be using.
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the port number of the internal server. (Will be used after restart.) <br>
   * The minimum port number is {@code 1025}, the maximum {@code 49151} and default is {@code 1337}.
   * 
   * @param port the port number to be set.
   */
  public void setPort(int port) {
    this.port = this.validateInt(port, 1025, 49151, this.port);
  }

  /**
   * Gets the character that separates the different sections of a Message from each other.<br>
   * (<code>U+001D</code>, 'Group separator'-character)
   */
  public char getDelimiter() {
    return delimiter;
  }

  public String getDbLocation() {
    return dbLocation;
  }

  /**
   * Set new location for the SQLite database file.
   * 
   * @param newLocation Requirements: - Must be a file. - Must be readable. - Must be writable.
   * @throws IllegalArgumentException If the string is empty, does not contain a file path or the
   *         file is not accessible.
   */
  public void setDbLocation(String newLocation) {
    if (newLocation == null || newLocation.isEmpty()) {
      throw new IllegalArgumentException("No location specified in Settings.setDbLocation(String).");
    }
    File testFile = new File(newLocation);
    if (!testFile.isFile() || !testFile.canRead() || !testFile.canWrite()) {
      throw new IllegalArgumentException("Specified location (" + newLocation
          + ") is no file or not readable/writable.");
    }
    dbLocation = newLocation;
  }

}
