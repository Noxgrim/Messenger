package manager;

import java.util.List;

import main.Core;
import coversations.Conversation;
import exceptions.DBException;
import exceptions.UnknownUuidException;

public class ConversationManager {

  private List<Conversation> conversations;

  private int active = 0;


  public Conversation getActiveConversation() {
    return conversations.get(active);
  }

  public Conversation getConversationByUuid(String uuid) throws UnknownUuidException {
    for (Conversation c : conversations)
      if (c.getUUID().equals(uuid))
        return c;
    try {
      Conversation c = Core.instance.getDatabase().getConversation(uuid);
      conversations.add(c);
      return c;
    } catch (DBException e) {
    }
    throw new UnknownUuidException("Couldn't find a Conversation with the UUID \"" + uuid + "\".");
  }

}
