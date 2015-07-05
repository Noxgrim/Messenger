package utils;

import java.util.regex.Pattern;

import main.Core;

/**
 * Contains the default formats of different Objects as Patterns and some "quick access" characters.
 */
public final class Formats {

  /** Character used to delimit Messages. */
  public static final char DELIMITER_CHAR = Core.getInstance().getSettings().getDelimiter();

  /**
   * Format of the Message's content:<br>
   * Mustn't contain the delimiter char.<br>
   * <br>
   * Regular expression:<br>
   * <code>"([^" + DELIMITER_CHAR + "]+)"</code>
   */
  public static final Pattern MESSAGE_FORMAT_CONTENT = Pattern.compile("([^" + DELIMITER_CHAR
      + "]+)");

  /**
   * Format of the Message's header:<br>
   * <code>delimiter</code> + time stamp in hex + <code>delimiter</code> + UUID of the Conversation
   * + <code>delimiter</code> + UUID of the sender + <code>delimiter</code> 'is command'-boolean as
   * number (0/1)<br>
   * <br>
   * Regular expression:<br>
   * <code>DELIMITER_CHAR
      + "([0-9a-f]+)" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([01])"</code>
   */
  public static final Pattern MESSAGE_FORMAT_HEADER = Pattern.compile(DELIMITER_CHAR
      + "([0-9a-f]+)" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([01])");

  /**
   * Format of a whole <i> not encrypted</i> Message:<br>
   * Connects <code>MESSAGE_FORMAT_HEADER</code> and <code>MESSAGE_FORMAT_CONTENT</code> with the
   * <code>delimiter</code>.<br>
   * <br>
   * Regular expression:<br>
   * <code>DELIMITER_CHAR
      + "([0-9a-f]+)" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})" + DELIMITER_CHAR
      + "([01])" + DELIMITER_CHAR + "([^" + DELIMITER_CHAR + "]+)" </code>
   */
  public static final Pattern MESSAGE_FORMAT = Pattern.compile(MESSAGE_FORMAT_HEADER.toString()
      + DELIMITER_CHAR + MESSAGE_FORMAT_CONTENT.toString());
  /**
   * Format of a whole <i>encrypted</i> Message:<br>
   * Three sections connected with the <code>delimiter</code> that don't contain it. <br>
   * <br>
   * Regular expression:<br>
   * <code>"([^" + DELIMITER_CHAR
      + "]+)" + DELIMITER_CHAR + "([^" + DELIMITER_CHAR
      + "]+)"</code>
   */
  public static final Pattern MESSAGE_ENCRYPTED = Pattern.compile("([^" + DELIMITER_CHAR + "]+)"
      + DELIMITER_CHAR + "([^" + DELIMITER_CHAR + "]+)");

  /**
   * Contains all special characters of a regular expression. Can be used to escape them.<br>
   * <br>
   * Regular expression:<br>
   * <code>"[\\{\\}\\(\\)\\[\\]\\.\\+\\*\\?\\^\\$\\\\\\|]"</code>
   */
  public static final Pattern SPECIAL_REGEX_CHARS = Pattern
      .compile("[\\{\\}\\(\\)\\[\\]\\.\\+\\*\\?\\^\\$\\\\\\|]");

  /**
   * Contains all escaped forms of the special characters of a regular expression. Can be used to
   * make them function again by removing the '\' before the character.<br>
   * <br>
   * Regular expression:<br>
   * <code>"\\\\([\\{\\}\\(\\)\\[\\]\\.\\+\\*\\?\\^\\$\\\\\\|])"</code>
   */
  public static final Pattern SPECIAL_REGEX_CHARS_ESCAPED = Pattern.compile("\\\\("
      + SPECIAL_REGEX_CHARS.toString() + ")");



  /**
   * Escapes the special regular expressions characters in a String to allow the use of them without
   * triggering the 'special powers' of this characters.
   * 
   * @param string the String to escape.
   * @return the escaped String.
   */
  public static String escapeRegex(String string) {

    return SPECIAL_REGEX_CHARS.matcher(string).replaceAll("\\\\$0");

  }

  /**
   * Turns escaped special regular expressions characters in a String back to their normal
   * appearance.
   * 
   * @param string the String to remove the escaped characters from.
   * @return the String with not escaped characters.
   */
  public static String unescapeRegex(String string) {
    return SPECIAL_REGEX_CHARS_ESCAPED.matcher(string).replaceAll("$1");
  }


}
