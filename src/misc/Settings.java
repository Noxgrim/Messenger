package misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import main.Core;
import exceptions.FormatException;

/**
 * A Settings object that will contain the settings of the program. <br>
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
public class Settings {
  /** The parent {@code Core} object */
  @IOHandler(save = false, load = false)
  private Core parent;

  /** The location of the configuration file. Can be set via Command line arguments. */
  @IOHandler(save = false, load = false)
  private String fileLocation;

  /**
   * The character that separates the different sections of a Message from each other.<br>
   * (<code>U+001D</code>, 'Group separator'-character)
   */
  private final char delimiter = '\u001D';

  /** The message length limit in characters. */
  private int msgLenLimit = 4096;
  /** The header length limit in characters. */
  private int headerLenLimit = 256;
  /** The nickname length limit in characters. */
  private int nickLenLimit = 64;
  /** The length of the generated session key in characters. */
  private int sessionKeyLen = 16;
  /** The socket timeout time in milliseconds. */
  private int connectionTimeout = 1000;

  /**
   * The boolean that determines if the program starts in terminal or GUI mode.
   */
  @IOHandler(getter = "getGuiMode")
  private boolean gui = false;
  /** The boolean that determines if exceptions will be printed. */
  @IOHandler(getter = "getPrintExceptions")
  private boolean exceptions = false;
  /**
   * The boolean that determines if debug messages will be printed. Can be set via Command line
   * arguments.
   */
  @IOHandler(save = false, load = false)
  private boolean debug = false;
  /** The boolean that determines if colors will be shown. */
  @IOHandler(getter = "getColorShown")
  private boolean color = true;

  /** The custom char set. If {@code "DEAFULT"} the {@code DEFAULT} char set will be loaded. */
  private String charSet = null;
  /** The default char set. */
  private static final String DEFAULT_CHAR_SET =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVXYZ!\"�$%&/()=?\\+#-.,*'_:;~";

  /** The nickname of the user. */
  private String ownNick = null;
  @IOHandler(load = false, comment = "Currrently unused.")
  private String host = null;
  /**
   * The port of the internal server.
   */
  private int port = 1337;
  
  /** Path to the SQLite database file.*/
  private String dbLocation;

  /**
   * Constructs a new {@code Settings } object and loads its values from the {@code messenger.cfg}
   * file.
   * 
   * @param core The parent {@code Core} object.
   */
  public Settings(Core core) {
    this(core, "load", "messenger.cfg");
  }

  /**
   * Constructs a new {@code Settings} object.<br>
   * <table>
   * <tr>
   * <th>Creation type</th>
   * <th>Description</th>
   * </tr>
   * <tr>
   * <td>{@code "load"}</td>
   * <td>Loads the {@code Settings} from the {@code messenger.cfg} file.</td>
   * </tr>
   * <tr>
   * <td>{@code "void"}</td>
   * <td>Creates a {@code Settings} object that has no values attached to it.</td>
   * </tr>
   * <tr>
   * <td>{@code "default"}</td>
   * <td>Creates a {@code Settings} object with the default values. (default action)</td>
   * </tr>
   * </table>
   * 
   * @param core The parent {@code Core} object.
   * @param creationType The type of the creation. See above.
   * @param fileLocation the location of the file that will be saved and loaded from.
   */
  public Settings(Core core, String creationType, String fileLocation) {
    this.parent = core;
    this.fileLocation = fileLocation;

    switch (creationType.toLowerCase()) {
      case "load":
        this.load();
        break;
      case "void":
        break;
      case "default":
        this.setToDefault(false);
        break;
      default:
        throw new IllegalArgumentException("\"" + creationType + "\" is not supported");
    }

  }

  /**
   * Constructs a {@code Settings} object from another.
   * 
   * @param from {@code Settings} used to construct this {@code Settings}.
   */
  public Settings(Settings from) {
    this.setToDefault(false);
    this.equalise(from);
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
   * The minimum value is {@code 8} and the default value is {@code 16}.
   * 
   * @param sessionKeyLen The session key length to be set.
   */
  public void setSessionKeyLen(int sessionKeyLen) {
    this.sessionKeyLen = this.validateInt(sessionKeyLen, 8, Integer.MAX_VALUE, this.sessionKeyLen);
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
   * Gets the selected char set.
   * 
   * @return the selected custom char set or {@code "DEFAULT"} for the default char set.
   */
  public String getCharSet() {

    return (this.charSet.equalsIgnoreCase("DEFAULT")) ? DEFAULT_CHAR_SET : this.charSet;

  }

  /**
   * Set the char set. <br>
   * If {@code "DEFAULT"} the {@code DEFAULT_CHAR_SET} will be selected, else the new char set will
   * be set. A char set can only contain each character once and cannot contain one of the control
   * characters.
   * 
   * @param customCharSet The char set to be set.
   */
  public void setCharSet(String customCharSet) {
    this.charSet = this.validateCharSet(customCharSet);
  }

  /**
   * Gets the user's nickname.
   */
  public String getOwnNick() {
    return ownNick;
  }

  /**
   * Sets the user's nickname.<br>
   * A nickname cannot be longer than the nickname length limit. A longer nickname will be trimmed.
   * 
   * @param ownNick the new nickname of the user.
   */
  public void setOwnNick(String ownNick) {
    this.ownNick = this.validateNick(ownNick, 1, this.nickLenLimit);
  }

  /**
   * Gets the host of the internal server.
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the host of the internal server. should be a IP or {@code "localhost"}.
   * 
   * @param host The host to be set.
   */
  public void setHost(String host) {
    this.host = validateHost(host);
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
   * @param newLocation
   *   Requirements: - Must be a file.
   *                 - Must be readable.
   *                 - Must be writable.
   * @throws IllegalArgumentException
   *   If the string is empty, does not contain a file path or the file is not accessible.
   */
  public void setDbLocation(String newLocation) {
    if (newLocation == null || newLocation.isEmpty()) {
      throw new IllegalArgumentException("No location specified in Settings.setDbLocation(String).");
    }
    File testFile = new File(newLocation);
    if (!testFile.isFile() || !testFile.canRead() || !testFile.canWrite()) {
      throw new IllegalArgumentException("Specified location is no file or not readable/writable.");
    }
    dbLocation = newLocation;
  }

  /**
   * Sets all values of the Settings object to the default ones and generates a new key pair. <br>
   * <br>
   * Defaults:<br>
   * <table>
   * <tr>
   * <th>Value</th>
   * <th>Default</th>
   * </tr>
   * <td>{@code  msgLenLimit}</td>
   * <td>{@code  4098}</td></tr>
   * <tr>
   * <td>{@code  headerLenLimit}</td>
   * <td>{@code 256}</td>
   * </tr>
   * <tr>
   * <td>{@code nickLenLimit}</td>
   * <td>{@code  64}</td>
   * </tr>
   * <tr>
   * <td>{@code sessionKeyLen}</td>
   * <td> {@code 16}</td>
   * </tr>
   * <tr>
   * <td>{@code connectionTimeout}</td>
   * <td>{@code 1000}</td>
   * </tr>
   * <tr>
   * <td>{@code gui}</td>
   * <td> {@code false}</td>
   * </tr>
   * <tr>
   * <td>{@code debug}</td>
   * <td>{@code false}</td>
   * </tr>
   * <tr>
   * <td>{@code exceptions}</td>
   * <td> {@code false}</td>
   * </tr>
   * <tr>
   * <td>{@code color}</td>
   * <td>{@code true}</td>
   * </tr>
   * <tr>
   * <td>{@code ownNick}</td>
   * <td> {@code "MissingNo"}</td>
   * </tr>
   * <tr>
   * <td>{@code charSet}</td>
   * <td>{@code "DEFAULT"}</td>
   * </tr>
   * <tr>
   * <td>{@code host}</td>
   * <td>{@code "localhost"}</td>
   * </tr>
   * <tr>
   * <td>{@code port}</td>
   * <td>{@code 1337}</td>
   * </tr>
   * <table>
   * <br>
   * 
   * @param save if set to true the changes will be saved.
   */
  public void setToDefault(boolean save) {

    this.msgLenLimit = 4098;
    this.headerLenLimit = 256;
    this.nickLenLimit = 64;
    this.sessionKeyLen = 16;
    this.gui = false;
    this.exceptions = false;
    this.debug = false;
    this.color = true;
    this.ownNick = "MissingNo";
    this.host = "localhost";
    this.port = 1337;

    this.charSet = "DEFAULT";
    // TODO Add generation of a new key pair.

    if (save) {
      this.save();
    }
  }

  /**
   * Saves the settings in a configuration file. If such a file does not exist it will be created.<br>
   * <br>
   * 
   * This method tries to save a field by invoking the corresponding <code>getter</code>-method. The
   * default getter name will be created with the following syntax:
   * 
   * <pre>
   * 'get' + field name
   * </pre>
   * 
   * If you want to avoid the saving of a field you can use the <code>save</code> value of
   * {@link IOHandler}, If your field has a getter but it has a slightly different name as the
   * default creation syntax you can specify it with the <code>getter</code> value of
   * {@link IOHandler}. This value has to be the exact name of the method (So it will not just
   * replace <code>field name</code> in the creation syntax.) Also, a comment will be saved behind
   * the value if <code>comment</code> is specified.<br>
   * <code>final</code> fields wont be saved.
   * 
   * @see IOHandler
   */
  public void save() {

    try (BufferedWriter br = new BufferedWriter(new FileWriter(new File(fileLocation)))) {

      // Write the date in the first line
      br.write('#' + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
          Locale.getDefault()).format(new Date()));


      for (Field f : this.getClass().getDeclaredFields()) {
        IOHandler ioH = f.getDeclaredAnnotation(IOHandler.class);

        String getter = "";
        String comment = "";
        String fieldName = f.getName();
        if (ioH != null) {
          if (!ioH.save() || Modifier.isFinal(f.getModifiers()))
            continue;
          getter = ioH.getter();

          if (getter.isEmpty())
            getter = "get" + fieldName;
          if (!comment.isEmpty())
            comment = " #" + comment;
          fieldName =
              Character.toUpperCase(fieldName.charAt(0))
                  + ((fieldName.length() == 1) ? "" : fieldName.substring(1, fieldName.length()));


          try {
            String value = this.getClass().getMethod(getter).invoke(this).toString();
            if (value.equals(DEFAULT_CHAR_SET))
              value = "DEFAULT";

            String name = "";
            for (char c : fieldName.toCharArray())
              if (Character.toUpperCase(c) == c)
                name += ("-" + c).toLowerCase();
              else
                name += c;
            if (name.startsWith("-"))
              name = name.substring(1, name.length());

            br.write('\n' + name + '=' + value + comment);

          } catch (NoSuchMethodException | SecurityException | IllegalAccessException
              | InvocationTargetException e) {
            parent.printError(null, e, false);
          }
        }

      }
      br.write("\n");
    } catch (IOException e) {
      parent.printError(null, e, false);
    }

  }

  /**
   * Loads the configuration file. If one of the keys is unknown or the value couldn't be parsed the
   * default settings will be loaded. If one of the values in invalid it will be replaced with the
   * corresponding default value. If the file does not exist it will be created and the default
   * values will be loaded.<br>
   * <br>
   * 
   * This method tries to load a field by invoking the corresponding <code>setter</code>-method. The
   * default setter name will be created with the following syntax:
   * 
   * <pre>
   * 'set' + field name
   * </pre>
   * 
   * If you want to avoid the loading of a field you can use the <code>load</code> value of
   * {@link IOHandler}, If your field has a setter but it has a slightly different name as the
   * default creation syntax you can specify it with the <code>setter</code> value of
   * {@link IOHandler}. This value has to be the exact name of the method (So it will not just
   * replace <code>field name</code> in the creation syntax.)<br>
   * <code>final</code> fields wont be loaded.
   * 
   * @see IOHandler
   */
  public void load() {

    this.setToDefault(false);

    File file = new File(fileLocation);
    if (!file.exists())
      this.save();
    else
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {

        String line;
        while ((line = br.readLine()) != null) {
          if (line.indexOf('#') != -1)
            line = line.substring(0, line.indexOf('#'));
          if (!line.startsWith("#") && line.split("=").length == 2) {
            line = line.trim();
            String value = line.split("=")[1];
            String name = "";
            {
              boolean isHump = true;

              for (char c : line.split("=")[0].toCharArray())
                if (isHump) {
                  name += Character.toUpperCase(c);
                  isHump = false;
                } else if (c == '-')
                  isHump = true;
                else
                  name += c;
            }

            String setter = "";
            try {
              String tmpName =
                  Character.toLowerCase(name.charAt(0))
                      + ((name.length() == 1) ? "" : name.substring(1, name.length()));

              Field f = this.getClass().getDeclaredField(tmpName);

              IOHandler ioH = f.getDeclaredAnnotation(IOHandler.class);

              if (!ioH.load() || Modifier.isFinal(f.getModifiers()))
                continue;

              setter = ioH.setter();

              if (setter.isEmpty() && !ioH.getter().isEmpty())
                if (ioH.getter().charAt(0) == 'g')
                  setter = 's' + ioH.getter().substring(1, ioH.getter().length());

            } catch (NoSuchFieldException e) {
              parent.printError("Could not find Field '" + name + "'!", e, false);
            }

            if (setter.isEmpty())
              setter = "set" + name;

            try {
              int i = 0;
              double d = 0.0;
              boolean b = false;

              Class<?> cla = null;

              if (value.equalsIgnoreCase("true")) {
                b = true;
                cla = boolean.class;
              } else if (value.equalsIgnoreCase("false"))
                cla = boolean.class;
              else if (value.contains(".")) {
                d = Double.parseDouble(value);
                cla = double.class;
              } else {
                i = Integer.parseInt(value);
                cla = int.class;
              }

              try {

                Method m = this.getClass().getMethod(setter, cla);

                if (cla.equals(boolean.class))
                  m.invoke(this, b);
                else if (cla.equals(int.class))
                  m.invoke(this, i);
                else
                  m.invoke(this, d);

              } catch (NoSuchMethodException | SecurityException | InvocationTargetException
                  | IllegalAccessException e) {
                parent.printError(null, e, false);
              }

            } catch (NumberFormatException isStringOrUnknownType) {
              try {
                this.getClass().getMethod(setter, String.class).invoke(this, value);

              } catch (NoSuchMethodException | SecurityException | InvocationTargetException
                  | IllegalAccessException e) {
                parent.printError(null, e, false);
              }
            }
          }
        }

      } catch (IOException e) {
        parent.printError(null, e, false);
      }
  }

  /**
   * Validates an integer.
   * 
   * @param i Integer to be validated
   * @param min Minimal allowed value. ({@code >=})
   * @param max Maximal allowed value. ({@code <=})
   * @param ifInvalid Value that will be returned if the integer is invalid.
   * @return The integer itself or the 'ifInvalid' value.
   */
  private int validateInt(int i, int min, int max, int ifInvalid) {
    if (i >= min && i <= max)
      return i;
    else
      try {
        throw new FormatException("Integer '" + i + "' is too "
            + ((i < min) ? "small (min: " + min : "big (max: " + max) + ").");
      } catch (FormatException e) {
        parent.printError(null, e, false);
      }

    return ifInvalid;

  }

  /**
   * Validates a char set.
   * 
   * @param customCharSet Char set to be validated.
   * @return {@code this.charSet} if {@code customCharSet} is {@code null} or empty. Illegal
   *         characters will be removed.
   */
  private String validateCharSet(String customCharSet) {
    if (customCharSet.isEmpty() || customCharSet == null) {
      try {
        throw new FormatException("The new char set cannot be empty or null.");
      } catch (FormatException e) {
        parent.printError(null, e, false);
      }
      return this.charSet;
    }
    if (customCharSet.equalsIgnoreCase("DEFAULT") || customCharSet.equals(DEFAULT_CHAR_SET))
      return "DEFAULT";
    else {
      String back = "";
      for (char c : customCharSet.toCharArray())
        if (back.indexOf(c) != -1 || c == this.delimiter)
          try {
            throw new FormatException(
                "The new char set cannot contain one of the control characters or a character twice.");
          } catch (FormatException e) {
            parent.printError(null, e, false);
          }
        else
          back += c;
      if (back.isEmpty()) {
        try {
          throw new FormatException("The new char set cannot be empty.");
        } catch (FormatException e) {
          parent.printError(null, e, false);
        }
        return this.charSet;
      }
      return back;
    }
  }

  /**
   * Validates a nickname.
   * 
   * @param nick The nickname to be validated.
   * @param minLen The minimal allowed length of the nickname in characters. (>=)
   * @param maxLen The maximal allowed length of the nickname in characters. (>=)
   * @return the nickname itself if it's valid, {@code "MissingNo"} if the name is too short and a
   *         shortened name if the name is too long.
   */
  private String validateNick(String nick, int minLen, int maxLen) {

    if (nick.length() >= minLen && nick.length() <= maxLen) {
      return nick;
    }
    if (nick.length() < minLen) {
      try {
        throw new FormatException("Nickname too short. (1 character minimum.)");
      } catch (FormatException e) {
        parent.printError(null, e, false);
      }
      return "MissingNo";
    }

    try {
      throw new FormatException("Nickname too long. (" + maxLen + " character"
          + ((maxLen == 1) ? "" : "s") + " maximum.)");
    } catch (FormatException e) {
      parent.printError(null, e, false);
    }
    return nick.substring(0, maxLen);

  }

  /**
   * Tests if the host is reachable.
   * 
   * @param host The host name to be validated.
   * @return the host String itself or the preset host string if invalid.
   */
  private String validateHost(final String host) {

    try {
      if (InetAddress.getByName(host.toLowerCase()).isReachable(10000))
        return host.toLowerCase();
    } catch (IOException e) {
      parent.printError(null, e, false);
    }

    return this.host;
  }

  /**
   * Equalises all values of this {@code Settings} object with the ones of another.
   * 
   * @param with The {@code Settings} object that is used as model.
   */
  public void equalise(Settings with) {

    this.setCharSet(with.getCharSet());
    this.setHost(with.getHost());
    this.setGuiMode(with.getGuiMode());
    this.setHeaderLenLimit(with.getHeaderLenLimit());
    this.setMsgLenLimit(with.getMsgLenLimit());
    this.setOwnNick(with.getOwnNick());
    this.setNickLenLimit(with.getNickLenLimit());
    this.setPort(with.getPort());
    this.setPrintExceptions(with.getPrintExceptions());
    this.setSessionKeyLen(with.getSessionKeyLen());
    this.setConnectionTimeout(with.getConnectionTimeout());
    this.setDebugMode(with.getDebugMode());
    this.setColorShown(with.getColorShown());

  }

  /**
   * Gets the default char set {@code DEFAULT_CHAR_SET}.
   */
  public String getDefaultCharSet() {
    return DEFAULT_CHAR_SET;
  }



  /**
   * Placed over a field, this annotation will specify whether the field will be loaded or saved and
   * can specify a comment behind the saved value. If the names of the getters and setters varies
   * from the name of the field the <code>getter</code> and <code>setter</code> values can be used
   * to specify them.<br>
   * If <code>setter</code> isn't set but <code>getter</code> is, the <code>load</code>-method will
   * generate it by switching the <code>s</code> with a <code>g</code>.<br>
   * Default values:
   * <ul>
   * <li> <code>load</code>: <code>true</code>
   * <li> <code>save</code>: <code>true</code>
   * <li> <code>comment</code>: <code>""</code>
   * <li> <code>getter</code>: <code>""</code>
   * <li> <code>setter</code>: <code>""</code>
   * </ul>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  private @interface IOHandler {
    /** Load this field. */
    boolean load() default true;

    /** Save this field. */
    boolean save() default true;

    /** The name of the setter-method of this field. */
    String setter() default "";

    /** The name of the getter-method of this field. */
    String getter() default "";

    /**
     * Comment that will be written behind the saved field value.
     */
    String comment() default "";
  }

}
