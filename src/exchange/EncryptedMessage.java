package exchange;

import java.util.regex.Matcher;

import persons.Contact;
import utils.Formats;
import exceptions.FormatException;

/**
 * Represent a encrypted Message.<br>
 * A encrypted Message is always encrypted for a specific Contact. The public key from the Contact
 * will be taken and the encrypted Message will be created.<br>
 * An encrypted Message has a session key, that is encrypted with a public key and the encrypted
 * content of it. Both sections are separated by the delimiter char.<br>
 * This Message type should only be used while sending and not internally.
 */
public class EncryptedMessage implements Message {
  /** The session key of this Message. */
  private String sessionKey;
  /** The encrypted content of this Message. */
  private String encrypted;

  public EncryptedMessage(String sessionKey, String encryptedContent) {
    encrypted = encryptedContent;
    this.sessionKey = sessionKey;
  }

  public EncryptedMessage(String formattedMsgString) throws FormatException {
    setFormatted(formattedMsgString);
  }

  @Override
  public EncryptedMessage toEncryptedMessge(Contact forContact) {
    return this.toInternalMessage().toEncryptedMessge(forContact);
  }

  @Override
  public InternalMessage toInternalMessage() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @return the formatted representation of this Encrypted Message. <br>
   * 
   *         <pre>
   * session key + (delimiter) + encrypted content
   * </pre>
   */
  @Override
  public String getFormatted() {
    return sessionKey + Formats.DELIMITER_CHAR + encrypted;
  }

  @Override
  public void setFormatted(String formattedMsgString) throws FormatException {
    Matcher m = Formats.MESSAGE_ENCRYPTED.matcher(formattedMsgString);

    if (m.matches()) {
      sessionKey = m.group(1);
      encrypted = m.group(2);
    } else
      throw new FormatException("Illegal EncryptedMessage format: '" + formattedMsgString + "'");
  }

  @Override
  public CommandMessage toCommandMessage() throws FormatException {
    // TODO Auto-generated method stub
    return null;
  }
}
