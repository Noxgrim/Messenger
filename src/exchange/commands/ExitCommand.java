package exchange.commands;

/**
 * A Command that exits the program.
 */
public class ExitCommand extends Command {
  
  public ExitCommand() {
    super("exit", null, "Shuts down the programm.", "Use this Command to shut down the program. (No arguments allowed).");
  }
  
  @Override
  public boolean perform(String[] args) {
    if (args.length == 0) {
      System.exit(0);
      return true;
    }
    else
      return false;
  }

}
