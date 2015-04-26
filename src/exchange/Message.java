package exchange;

import persons.Contact;
import exceptions.FormatException;

/**
 * Represents a message that can be sent between users.<br>
 * It contains the UUID of the sender and conversation.
 */
public interface Message {

  /**
   * @param forContact The Contact this Message will be encrypted for. Their public key will be used
   *        to encrypt the Message.
   * @return the encrypted representation of this Message.
   */
  public EncryptedMessage toEncryptedMessge(Contact forContact);

  /**
   * @return the <b>not</b> encrypted representation of this Message.
   */
  public InternalMessage toInternalMessage();

  /**
   * @return the command form of this Message. While conversion the first word will be interpreted
   *         as command name and the following ones as arguments. If the Message starts with a '/'
   *         the character will be removed.
   */
  public CommandMessage toCommandMessage();


  /**
   * @return the formatted Message String of this Message.
   */
  public String getFormatted();

  /**
   * Sets the Message to the specified formatted Message String.
   * 
   * @param formattedMsgString The formatted Message String.
   * @throws FormatException if the format of the <code>formattedMsgString</code> is invalid.
   */
  public void setFormatted(String formattedMsgString) throws FormatException;



}
