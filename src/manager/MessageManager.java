package manager;

import java.security.InvalidKeyException;
import java.util.Objects;

import main.Core;
import persons.Contact;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.EncryptedMessage;
import exchange.InternalMessage;

public class MessageManager {

  private CommandManager ci;

  private ConversationManager conMngr;

  public MessageManager(ConversationManager conMngr) {
    this.conMngr = conMngr;

    ci = new CommandManager();
    ci.registerDefaults();
  }

  public MessageManager(ConversationManager conMngr, CommandManager cm) {
    this.conMngr = conMngr;
    this.ci = cm;
  }

  public void interpreteIncomingMessage(EncryptedMessage m) {

    InternalMessage im = null;

    try {

      im = m.toInternalMessage();

      if (im.isCommand())
        if (Core.instance.getUserInterface().confirmDialog(
            Core.instance.getDatabase().getContact(im.getUuidSender()).getNickname()
                + " wants to execute the following command: \"" + im.getContent()
                + "\"\nDo you wnt to execute it?", false))
          ci.runMessageCommand(im);

        else {
          // conMngr.getConversationByUuid(im.getUuidConversation()); //TODO Do Stuff here.
        }



    } catch (InvalidKeyException | FormatException e) {

    } catch (DBException e) {
      Core.instance.getUserInterface().printError(
          "Unable to find Contact with the UUID \"" + im.getUuidSender() + "\"!", e);
      e.printStackTrace();
    }
  }
  
  public boolean interpreteOutgoingMessage(InternalMessage im, Contact forContact) {

    
    EncryptedMessage em = null;
    try {
      em = im.toEncryptedMessge(forContact);
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    if (!Core.instance.getClient().sendMessage(em, forContact.getAddress())) {
      Core.instance.getUserInterface().printError("Couldn't sent Message.");
      return false;
    }
    return true;
  }

}
