package userInterface;

import persons.Contact;

public interface ConversationManager {

  public void switchToCoversation(Contact contact);

  public void addConversation(Contact contact);

  public void removeConverstaion(Contact contact);

}
