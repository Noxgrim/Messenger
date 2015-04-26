package userInterface;

import main.Core;

/**
 * Represents a ASCII color code. Is empty, if the target machine runs windows. (Windows cmd.exe
 * does not support ASCII color codes.)
 */
public enum TextColor {

  BLACK("\u001B[30m"), GRAY("\u001B[1;30m"), LIGHT_GRAY("\u001B[37m"), RED("\u001B[31m"), LIGHT_RED(
      "\u001B[1;31m"), GREEN("\u001B[32m"), LIGHT_GREEN("\u001B[1;32m"), GOLD("\u001B[33m"), YELLOW(
      "\u001B[1;33m"), BLUE("\u001B[34m"), LIGHT_BLUE("\u001B[1;34m"), PURPLE("\u001B[35m"), PINK(
      "\u001B[1;35m"), CYAN("\u001B[36m"), LIGHT_CYAN("\u001B[1;36m"), WHITE("\u001B[1;37m"),

  BACKGROUND_BLACK("\u001B[40m"), BACKGROUND_RED("\u001B[41m"), BACKGROUND_GREEN("\u001B[42m"), BACKGROUND_GOLD(
      "\u001B[43m"), BACKGROUND_BLUE("\u001B[44m"), BACKGROUND_PURPLE("\u001B[45m"), BACKGROUND_CYAN(
      "\u001B[46m"), BACKGROUND_LIGHT_GRAY("\u001B[47m"),

  RESET("\u001B[0m"),
  /**
   * Sets the text to bold/bright text.<br>
   * Use {@code RESET} to reset it.
   */
  BOLD("\u001B[1m");



  private final String ESC_SEQUENCE;


  private TextColor(String escSequence) {
    ESC_SEQUENCE = escSequence;
  }

  @Override
  public String toString() {
    return (Core.getInstance() == null) ? ""
        : (Core.getInstance().getSettings().getColorShown()) ? ESC_SEQUENCE : "";
  }

}
