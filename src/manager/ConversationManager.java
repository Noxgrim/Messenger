package manager;

import java.util.List;
import coversations.Conversation;

public class ConversationManager {
  
  private List<Conversation> conversations;
  
  private int active = 0;
  
  
  public Conversation getActiveConversation() {
    return conversations.get(active);
  }
  
}
