package exchange.commands;

import exceptions.FormatException;
import exchange.InternalMessage;
import main.Core;

/**
 * Can be used to send a Message with a leading slash (normally indicator for a Command and leading
 * word will be interpreted as one) to all participants of the active Conservation.
 */
public class SlashEscapeCommand extends Command {
  public SlashEscapeCommand() {
    super("/", "Usage: '// <message>'", "Sends a Message with an leading '/'.",
        "Sends a Message with an leading '/'. The space after '//' will be ignored.");
  }

  @Override
  public boolean perform(String[] args) {
    String msg = "";
    for (String s : args)
      msg += s + ' ';
    msg.substring(0, msg.length() - 1);

    try {
      return Core.getInstance().getConversationManager().getActiveConversation()
          .sendMessage(new InternalMessage('/' + msg, Core.getInstance().getConversationManager().getActiveConversation().getUUID(), Core.getInstance().getUser().getUUID(), false));
    } catch (FormatException e) {
      Core.getInstance().getUserInterface()
          .printError("An internal Error occured while performing this Command!", e);
      return false;
    }
  }
}
