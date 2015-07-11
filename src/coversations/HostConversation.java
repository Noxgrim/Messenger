package coversations;

import java.util.ArrayList;
import java.util.List;

import main.Core;
import persons.Contact;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.InternalMessage;

public class HostConversation extends Conversation {

  private List<Contact> participants = new ArrayList<Contact>();

  public HostConversation(Contact c) {
    this(c, c.getNickname());
  }
  
  /**
   * Constructs a new Host Conversation.
   * @param participants The participants of this Conversation.
   * @param uuid The UUID of this Conversation.
   * @param name The name of this Conversation
   */
  public HostConversation(List<Contact> participants, String uuid, String name) {
    super(uuid, name);
    this.participants = participants;
  }
  
  public HostConversation(String uuid, String name, List<String> participants) throws DBException {
    super(uuid, name);
    
    this.participants = new ArrayList<Contact>();
    
    for (Contact c: Core.instance.getDatabase().getContacts())
      for (String s : participants)
        if (c.getUUID().equals(s) && !this.participants.contains(c))
          this.participants.add(c);
  }

  public HostConversation(Contact c, String name) {
    super(java.util.UUID.randomUUID().toString(), c.getNickname());
    participants.add(c);
    this.name = name;
  }

  /**
   * @return the participants of the Conversation.
   */
  public List<Contact> getParticipants() {
    return participants;
  }

  /**
   * @param participents the participants to set.
   */
  public void setParticipants(List<Contact> participents) {
    this.participants = participents;
  }

  /**
   * Adds a specific contact to a Conversation if the Contact is not a participant.
   * 
   * @param paticipant Contact to add.
   */
  public void addPaticipant(Contact paticipant) {
    if (!participants.contains(paticipant) && paticipant != null)
      participants.add(paticipant);
  }

  /**
   * Removes a Contact from a conversation.
   * 
   * @param paticipant Contact to remove.
   */
  public void removeParticipant(Contact paticipant) {
    participants.remove(paticipant);
  }

  /**
   * Sends a Message to all participants of a Conversation as the user.
   * 
   * @param message message to send.
   * @return whether the sending has suicided (all participants).
   * @throws FormatException if the message is invalid.
   */
  public boolean sendMessage(String message) throws FormatException {
    return this.sendMessage(new InternalMessage(message, this.uuid, Core.getInstance().getUser().getUUID()));
  }

  /**
   * Sends a Message to all participants of a Conversation.
   * 
   * @param message Message to send.
   * @return whether the sending has suicided (all participants).
   * @throws FormatException if the message is invalid.
   */
  public boolean sendMessage(InternalMessage m) {
    boolean result = true;

    for (Contact c : participants)
      if (!Core.instance.getMessageManager().interpreteOutgoingMessage(m, c)) {
        result = false;
        Core.getInstance().getUserInterface()
            .printError("Sending to '" + c.getNickname() + "' failed.");
      }

    return result;
  }

  @Override
  public List<String> getParticipantsIds() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void logMessage(InternalMessage im) {
    // TODO Auto-generated method stub
    
  }

}
