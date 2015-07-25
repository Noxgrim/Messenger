package exchange;

import main.Core;
import persons.Contact;
import exceptions.FormatException;

public class CommandMessage implements Message {

  String name;

  String[] arguments;

  @Override
  public EncryptedMessage toEncryptedMessge(Contact forContact) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InternalMessage toInternalMessage() throws FormatException {
    String arguments = "";

    for (String argument : this.arguments)
      arguments += " " + argument;

    try {
      return new InternalMessage("/" + name + arguments, Core.getInstance()
          .getConversationManager().getActiveConversation().getUuid(), Core.getInstance().getUser()
          .getUuid());
    } catch (FormatException e) {
      Core.getInstance().getUserInterface().printError(e);
    }
    return null;
  }

  @Override
  public CommandMessage toCommandMessage() throws FormatException {
    return this;
  }

  @Override
  public String getFormatted() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setFormatted(String formattedMsgString) throws FormatException {
    // TODO Auto-generated method stub

  }

}
