package coversations;

import java.util.LinkedList;
import java.util.List;

import main.Core;
import persons.Contact;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.InternalMessage;

public class GuestConversation extends Conversation {

  private Contact host;

  public GuestConversation(Contact host, String uuid, String name) {
    super(uuid, name);
    this.host = host;
  }
  
  public GuestConversation(String uuid, String name, List<String> participantsUuids) {
    super(uuid, name);
  }

  /**
   * @return the host of the Conversation.
   */
  public Contact getHost() {
    return host;
  }

  @Override
  public boolean sendMessage(String message) throws FormatException {
    return sendMessage(new InternalMessage(message, this.uuid, Core.getInstance().getUser()
        .getUuid()));
  }

  @Override
  public boolean sendMessage(InternalMessage internalMessage) throws FormatException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<String> getParticipantsUuids() {
    List<String> participantsUuids = new LinkedList<String>();
    participantsUuids.add(host.getUuid());
    return participantsUuids;
  }
  
  @Override
  public void saveMessage(InternalMessage im) {
    try {
      Core.instance.getDatabase().addMessage(im);
    } catch (DBException e) {
      Core.instance.getUserInterface(); //TODO Do Stuff!!!!11!
    }
  }
}
