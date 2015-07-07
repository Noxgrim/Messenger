package manager;

import java.util.ArrayList;
import java.util.List;

import userInterface.UserInterface;
import utils.Formats;
import main.Core;
import exchange.InternalMessage;
import exchange.commands.Command;
import exchange.commands.ExitCommand;
import exchange.commands.SlashEscapeCommand;

public class CommandManager {

  private List<Command> cmds = new ArrayList<Command>();

  public CommandManager() {}
  
  public CommandManager(List<Command> commands) {
    for (Command c : commands)
      registerCommand(c);
  }

  /**
   * Converts an given Message to a Command and tries to perform it.<br>
   * If the given Command does not exists <code>"Unknown Command."</code> will be printed to the
   * user.
   * 
   * @param m the Message that will be performed.
   */
  public boolean runMessageCommand(InternalMessage m) {
    UserInterface ui = Core.instance.getUserInterface();
    
    if (!m.isCommand()) {
      ui.printError("Message isn't a Command!");
      return false;
    }

    String[] commandContent = Formats.escapeRegex((m.getContent())).split(" ");
    for (int i = 0; i < commandContent.length; i++)
      commandContent[i] = Formats.unescapeRegex(commandContent[i]);

    String label = commandContent[0];

    Command target = null;

    for (Command cmd : cmds)
      if (cmd.getName().equalsIgnoreCase(label))
        target = cmd;
      else
        for (String alias : cmd.getAliases())
          if (alias.equals(target))
            target = cmd;
      

    if (target == null) {
      ui.printError("Unknown Command.");
      return false;
    }
      
    else {
      String[] args = new String[commandContent.length - 1];

      for (int i = 1; i < commandContent.length; i++)
        args[i - 1] = commandContent[i];

      return target.perform(args);
    }
  }

  /**
   * Adds a Command to the existing ones.<br>
   * The name and aliases of the Command have to be unique.
   * 
   * @param c The Command to be added.
   */
  public void registerCommand(Command c) {

    for (Command cmd : cmds)
      if (cmd.getName().equalsIgnoreCase(c.getName())) {
        Core.instance
            .getUserInterface()
            .printError(
                "Cannot add Command '" + c.getName() + "' (name conflict with '"
                    + cmd.getClass().getSimpleName() + "').");
        return;
      } else
        for (String alias : cmd.getAliases())
          for (String alias2 : c.getAliases())
            if (alias.equals(alias2)) {
              Core.instance
                  .getUserInterface()
                  .printError(
                      "Cannot add Command '" + c.getName() + "' (alias name conflict with '"
                          + cmd.getClass().getSimpleName() + "').");
              return;
            }

    cmds.add(c);
  }
  
  public void registerDefaults() {
    registerCommand(new SlashEscapeCommand());
    registerCommand(new ExitCommand());
  }

}
