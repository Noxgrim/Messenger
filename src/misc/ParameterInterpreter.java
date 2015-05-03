package misc;

import java.util.ArrayList;
import java.util.List;

import main.Core;
import misc.Settings.FieldData;

/**
 * Interprets given Parameters.
 */
public class ParameterInterpreter {

  private static List<Parameter> parameters = new ArrayList<Parameter>();

  /**
   * Represents a command line parameter.
   */
  abstract static public class Parameter {

    /** The long parameter name. */
    private String longName;
    /** The short parameter name. */
    private char shortName;
    /** The string that will be returned if the user uses the help command. */
    private String help;

    /** Minimum required arguments. */
    private int minRequiredArgs;
    /** Maximum required arguments. */
    private int maxRequiredArgs;
    /** Whether this parameter needs to be the last one in a parameter chain. */
    private boolean lastReqired;


    /**
     * Constructs a new <code>Parameter</code>.
     * 
     * @param longName The long name of the parameter. (without leading "<code>--</code>")
     * @param shortName the short name of the parameter. (without leading "<code>-</code>")
     * @param help the help String of the parameter.
     * @param minRequiredArgs the minimum count of the arguments.
     * @param maxRequiredArgs the maximum count of the arguments.
     * @param lastReqired whether this parameter has to be the last one in a parameter chain.
     * @throws IllegalArgumentException if one of the arguments is illegal.
     */
    public Parameter(String longName, char shortName, String help, int minRequiredArgs,
        int maxRequiredArgs, boolean lastReqired) throws IllegalArgumentException {

      setLongName(longName);
      setShortName(shortName);
      setHelp(help);

      if (minRequiredArgs < 0 || minRequiredArgs > maxRequiredArgs)
        throw new IllegalArgumentException(
            "Cannot be smaller than 0 or bigger than the maximal required argument count.");
      setMaxRequiredArgs(maxRequiredArgs);

      this.lastReqired = lastReqired;
    }

    /**
     * @return the long name of the parameter.
     */
    public String getLongName() {
      return longName;
    }

    /**
     * @return the short name of the parameter.
     */
    public char getShortName() {
      return shortName;
    }

    /**
     * @return the help string.
     */
    public String getHelp() {
      return help;
    }

    /**
     * @return whether this parameter has to be the last one of a parameter chain.
     */
    public boolean isLastRequied() {
      return lastReqired;
    }

    /**
     * @return the minimum required arguments.
     */
    public int getMinRequiredArgs() {
      return minRequiredArgs;
    }

    /**
     * @return the maximum required arguments.
     */
    public int getMaxRequiredArgs() {
      return maxRequiredArgs;
    }

    /**
     * @return whether this parameter can be used without arguments. Useful for parameter chains.
     */
    public boolean isStandAlone() {
      return minRequiredArgs == 0;
    }

    /**
     * @return whether this parameter cannot have any arguments.
     */
    public boolean isArgumentless() {
      return minRequiredArgs == 0 && maxRequiredArgs == 0;
    }

    /**
     * @return whether this parameter has a short name.
     */
    public boolean hasShortName() {
      return shortName != '\0';
    }

    /**
     * @param maxRequiredArgs the maximum required argument count to be set.
     * @throws IllegalArgumentException if <code>maxRequiredArgs</code> is smaller than
     *         <code>0</code> or <code>minRequiredArgs</code>.
     */
    public void setMaxRequiredArgs(int maxRequiredArgs) throws IllegalArgumentException {
      if (maxRequiredArgs < 0 || maxRequiredArgs < minRequiredArgs)
        throw new IllegalArgumentException(
            "Cannot be smaller than 0 or the minimal required argument count.");
      this.maxRequiredArgs = maxRequiredArgs;
    }

    /**
     * @param minRequiredArgs the minimum required argument count to be set.
     * @throws IllegalArgumentException if <code>minRequiredArgs</code> is smaller than
     *         <code>0</code> or bigger than <code>maxRequiredArgs</code>.
     */
    public void setMinRequiredArgs(int minRequiredArgs) throws IllegalArgumentException {
      if (minRequiredArgs < 0 || minRequiredArgs > maxRequiredArgs)
        throw new IllegalArgumentException(
            "Cannot be smaller than 0 or bigger than the maximal required argument count.");
      this.minRequiredArgs = minRequiredArgs;
    }

    /**
     * @param shortName the short name to be set. '<code>\0</code>' is used to indicate that this
     *        parameter has no short name.
     * @throws IllegalArgumentException if <code>shortName</code> is a ISO control character or is a
     *         '<code>-</code>'.
     */
    public void setShortName(char shortName) throws IllegalArgumentException {
      if (shortName == '\0') {
        this.shortName = shortName;
        return;
      }

      if (Character.isISOControl(shortName))
        throw new IllegalArgumentException("Short name cannot be a control character.");
      if (shortName == '-')
        throw new IllegalArgumentException("Short name cannot be '-'.");

      this.shortName = shortName;
    }

    /**
     * @param longName the long name to be set.
     * @throws IllegalArgumentException if <code>longName</code> is <code>null</code>, empty or
     *         contains spaces.
     */
    public void setLongName(String longName) throws IllegalArgumentException {
      if (longName == null || longName.isEmpty() || longName.contains(" "))
        throw new IllegalArgumentException("Name cannot be empty nor contain spaces.");
      if (longName.startsWith("-"))
        throw new IllegalArgumentException("Name cannot start with a '-'.");

      this.longName = longName;
    }

    /**
     * @param help Help String to be set. If <code>null</code> or empty the String will be set to
     *        <code>"&ltNo help available.&gt"</code>.
     */
    public void setHelp(String help) {
      if (help == null || help.isEmpty())
        help = "<No help available.>";
      this.help = help;
    }

    /**
     * Invokes a parameter with the given arguments.
     */
    abstract public void invoke(String[] args);
  }
  
  private static void initSettingsParameters(Core c, Settings s) {
    
    for (FieldData fd :  FieldData.getAccessibleFields(Settings.class))
      parameters.add(new Parameter(fd.getSavedSpelling(), '\0', "Changes the corresponding setting.", 1, 1, false) {
        
        @Override
        public void invoke(String[] args) {
          // TODO Auto-generated method stub
          
        }
      });
  }

  private static void initDefaultParameters(Core c, Settings s) {
    parameters.add(new Parameter("color", 'c',
        "Activates colors in the terminal version of the Program.", 0, 0, false) {
      @Override
      public void invoke(String[] args) {
        s.setColorShown(false);
      }
    });
    parameters.add(new Parameter("terminal", 't',
        "Starts the Program in the terminal user interface.", 0, 0, false) {
      @Override
      public void invoke(String[] args) {
        s.setGuiMode(false);
      }
    });
    parameters.add(new Parameter("gui", 'g', "Starts the Program in the graphical user interface.",
        0, 0, false) {
      @Override
      public void invoke(String[] args) {
        s.setGuiMode(true);
      }
    });
    parameters.add(new Parameter("port", 'p', "Sets the port for the server.", 1, 1, true) {
      @Override
      public void invoke(String[] args) {
        try {
          s.setPort(Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
          c.printError("Not an valid number (\"" + args[0] + "\")", e, false);
        }
      }
    });
    parameters.add(new Parameter("config-file-path", '\0',
        "Set the path of the config file to be loaded.", 1, 1, false) {
      @Override
      public void invoke(String[] args) {
        // TODO
      }
    });
    parameters.add(new Parameter("help", 'h',
        "Displays this help page and exits.\n    Can be used to get the help for a specific parameter", 0, 1,
        true) {
      @Override
      public void invoke(String[] args) {
        boolean printSpecific = false;
        if (args.length == 1)
          printSpecific = true;

        for (Parameter p : parameters)
          if (printSpecific && !p.getLongName().equalsIgnoreCase(args[0]))
            continue;
          else
            System.out.println("--" + p.getLongName() + " / -" + p.getShortName() + ":\n    "
                + p.getHelp());
      }
    });
  }

  /**
   * Interprets the command line arguments of the java program.
   * 
   * @param core The core object that has to accept the changes.
   * @param arguments The arguments of the program.
   */
  public static Settings interpretCommandLine(Core core, String[] arguments) {

    Settings s = new Settings(core);

    initDefaultParameters(core, s);


    for (int i = 0; i < arguments.length; i++) {

      switch (arguments[i].toLowerCase()) {

        case "-um":
        case "--ui-mode":

          if (i + 1 >= arguments.length || arguments[i + 1].charAt(0) == '-') {
            core.printError("\"--ui-mode\" needs a parameter.", null, false);
            break;
          }
          switch (arguments[i + 1].toLowerCase()) {
            case "gui":
              core.getSettings().setGuiMode(true);
              break;
            case "terminal":
              core.getSettings().setGuiMode(false);
              break;
            default:
              core.printError("Unsupported \"--ui-mode\" parameter.", null, false);
          }

          i++;
          break;

        case "-fc":
        case "--force-color":
          core.getSettings().setColorShown(true);
          break;

        case "-p":
        case "--port":
          if (i + 1 >= arguments.length || arguments[i + 1].charAt(0) == '-') {
            core.printError("\"--port\" needs a parameter.", null, false);
            break;
          }

          try {
            core.getSettings().setPort(Integer.parseInt(arguments[i + 1]));
          } catch (NumberFormatException e) {
            core.printError("Parameter of \"--port\" needs to be an integer value.", e, false);
          }

          i++;
          break;

        case "-?":
        case "--help":
          if (i + 1 >= arguments.length || arguments[i + 1].charAt(0) == '-')
            printCommandLineArgumentHelp(null);
          else {
            printCommandLineArgumentHelp(arguments[i + 1]);
            i++;
          }
          break;

      }

    }

    return s;

  }

  /**
   * Prints the command line argument help.
   */
  private static void printCommandLineArgumentHelp(String argument) {

  }

}
