package coversations;

import java.util.List;

import exceptions.FormatException;
import exchange.InternalMessage;

/**
 * Represent a Conversation between two or more persons.<br>
 * A Conversation has a UUID and a name. The name is an user-friendly way to specify the Conversation.
 */
public abstract class Conversation {
  /** The user-friendly name. */
  protected String name;
  /** The UUID. */
  protected String uuid;
  
  
  /**
   * Constructs a new Conversation with a given name and UUID.
   * @param uuid The UUID of the Conversation.
   * @param name The name of the Conversation.
   */
  public Conversation(String uuid, String name) {
    this.uuid = uuid;
    this.name = name;
  }
  

  /**
   * Sends a Message to a Conversation. The String will be interpreted as the content of the Message
   * and the UUID will be set to the User's one.
   * 
   * @param message The Message content to be sent.
   * @return whether the sending was successful.
   * @throws FormatException if the Message format is invalid.
   */
  abstract public boolean sendMessage(String message) throws FormatException;

  /**
   * Sends a Message to a Conversation.
   * 
   * @param internalMessage The Message to be sent.
   * @return whether the sending was successful.
   * @throws FormatException if the Message format is invalid.
   */
  abstract public boolean sendMessage(InternalMessage internalMessage) throws FormatException;

  public void loadFromDatabase(int count) {
    // TODO Database Logic.
  }

  /**
   * @return the user-friendly name of the Conversation. 
   */
  public String getName() {
    return name;
  }
  
  /**
   * @return the UUID of the Conversation.
   */
  public String getUUID() {
    return uuid;
  }
  
  /**
   * @return whether this Conversation is a HostConversation.
   */
  public boolean isHost() {
    return this instanceof HostConversation;
  }
  
  /**
   * @return get the IDs of the participants of this Conversation.
   */
  abstract public List<String> getParticipantsIds();

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    else if (obj instanceof Conversation)
      if (uuid.equals(((Conversation) obj).uuid))
        return true;
    return false;
  }



}
