package manager;

import java.util.ArrayList;
import java.util.List;

import utils.Formats;
import main.Core;
import exchange.InternalMessage;
import exchange.commands.Command;
import exchange.commands.ExitCommand;
import exchange.commands.SlashEscapeCommand;

public class CommandManager {

  private List<Command> cmds = new ArrayList<Command>();

  public CommandManager() {
    registerCommand(new SlashEscapeCommand());
    registerCommand(new ExitCommand());
  }

  /**
   * Converts an given Message to a Command and tries to perform it.<br>
   * If the given Command does not exists <code>"Unknown Command."</code> will be printed to the
   * user.
   * 
   * @param m the Message that will be performed.
   */
  public void runMessageCommand(InternalMessage m) {
    if (!m.isCommand())
      throw new IllegalArgumentException("Message isn't a Command!");

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
      

    if (target == null)
      Core.getInstance().getUserInterface().printError("Unknown Command.");
    else {
      String[] args = new String[commandContent.length - 1];

      for (int i = 1; i < commandContent.length; i++)
        args[i - 1] = commandContent[i];

      target.perform(args);
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
        Core.getInstance()
            .getUserInterface()
            .printError(
                "Cannot add Command '" + c.getName() + "' (name conflict with '"
                    + cmd.getClass().getSimpleName() + "').");
        return;
      } else
        for (String alias : cmd.getAliases())
          for (String alias2 : c.getAliases())
            if (alias.equals(alias2)) {
              Core.getInstance()
                  .getUserInterface()
                  .printError(
                      "Cannot add Command '" + c.getName() + "' (alias name conflict with '"
                          + cmd.getClass().getSimpleName() + "').");
              return;
            }

    cmds.add(c);
  }

}
