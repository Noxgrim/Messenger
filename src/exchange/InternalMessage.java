package exchange;

import java.util.GregorianCalendar;
import java.util.regex.Matcher;

import coversations.security.HybridCoder;
import main.Core;
import persons.Contact;
import utils.Formats;
import exceptions.FormatException;

/**
 * A Message that can be sent between users.<br>
 * This Message is <i>not</i> encrypted, so it shouldn't be used externally.<br>
 * It consists of the UUID of the Conversation and Message's sender. The UUIDs are (obviously)
 * needed to assign the right Conversation and Contact. A Message has also a time stamp (in the form
 * of a <code>GregorianCalendar</code>) and a <code>command</code>-flag. If <code>command</code> is
 * set, the Message will be interpreted as a Command. Even if the <code>command</code> is set the
 * Message has to start with a ' <code>/</code>'.<br>
 * The <code>getEncryptedMessage</code> can be used to easily encrypt a given Message.
 * @see EncryptedMessage
 * @see Message
 * @see Formats
 */
public class InternalMessage implements Message, Comparable<InternalMessage> {

  /** Content of a Message. */
  private String content;
  /** Whether the Message is a Command. */
  private boolean command;
  /** UUID of the sender. */
  private String uuidSender;
  /** The UUID of the Conversation this Message belongs to. */
  private String uuidConversation;
  /** The time this Message was created. */
  private GregorianCalendar timeStamp;
  /** 
   * Whether the Message has been sent.<br>
   * (Will not be saved in the formatted Message String.)
   */
  private boolean sent = false;


  /**
   * Constructs a new <code>Message</code>.
   * 
   * @param text The content of the Message
   * @param uuidConversation The UUID of the Conversation.
   * @param uuidSender The UUID of the sender.
   * @param command Whether the Message is a command.
   * @param timeStamp The time stamp of the Message.
   * @throws FormatException if the content length is invalid.
   */
  public InternalMessage(String text, String uuidConversation, String uuidSender, boolean command,
      long timeStamp) throws FormatException {

    if (!contentIsValid(text)) {
      throw new FormatException("Message invalid.");
    }

    content = text;
    this.uuidConversation = uuidConversation;
    this.uuidSender = uuidSender;
    this.command = command;

    this.timeStamp = new GregorianCalendar();
    this.timeStamp.setTimeInMillis(timeStamp);
  }

  /**
   * Constructs a new <code>Message</code> with the current time.
   * 
   * @param text The content of the Message
   * @param uuidConversation The UUID of the Conversation.
   * @param uuidSender The UUID of the sender.
   * @param command Whether the Message is a command.
   * @throws FormatException if the content length is invalid.
   */
  public InternalMessage(String text, String uuidConversation, String uuidSender, boolean command)
      throws FormatException {
    this(text, uuidConversation, uuidSender, command, System.currentTimeMillis() / 1000L);
  }

  /**
   * Constructs a new <code>Message</code> with the current time. The message is marked as a command
   * if the first character is a '\'.
   * 
   * @param text The content of the Message
   * @param uuidConversation The UUID of the Conversation.
   * @param uuidSender The UUID of the sender.
   * @throws FormatException if the content length is invalid.
   */
  public InternalMessage(String text, String uuidConversation, String uuidSender)
      throws FormatException {
    this(text, uuidConversation, uuidSender, text.charAt(0) == '/',
        System.currentTimeMillis() / 1000L);
  }


  /**
   * Constructs a new <code>Message</code> from a formatted message String.<br>
   * Format:
   * {@code (Delimiter)+time stamp+(Delimiter)+(Delimiter)+CoversationUUID+(Delimiter)+SenderUUID+(delimiter)+command+(Delimiter)+content}
   * 
   * @param formattedMsgString The formatted message String.
   * @throws FormatException if the formatted message String's format is invalid.
   */
  public InternalMessage(String formattedMsgString) throws FormatException {
    this.setFormatted(formattedMsgString);
  }

  /**
   * @return the formated representation as a {@code String}.<br>
   *         Format:<br>
   *         {@code (Delimiter)+time stamp+(Delimiter)+CoversationUUID+(Delimiter)+SenderUUID+(delimiter)+command+(Delimiter)+content}
   */
  public String getFormatted() {
    String result = "";

    result += Formats.DELIMITER_CHAR;
    result += Long.toHexString(timeStamp.getTimeInMillis() / 1000);
    result += Formats.DELIMITER_CHAR;
    result += uuidConversation;
    result += Formats.DELIMITER_CHAR;
    result += uuidSender;
    result += Formats.DELIMITER_CHAR;
    result += command ? 1 : 0;
    result += Formats.DELIMITER_CHAR;
    result += content;

    result = Formats.escapeRegex(result);

    return result;
  }

  /**
   * @return whether the {@code Message} is a command.
   */
  public boolean isCommand() {
    return command;
  }
  
  /**
   * @return whether this Message has been sent.
   */
  public boolean isSent() {
    return sent;
  }

  /**
   * @return the UUID of the sender.
   */
  @Override
  public String getUuidSender() {
    return uuidSender;
  }
  
  /**
   * @return the UUID of the Conversation.
   */
  public String getUuidConversation() {
    return uuidConversation;
  }
  
  /**
   * @return the time stamp of this Message.
   */
  public GregorianCalendar getTimeStamp() {
    return timeStamp;
  }
  
  /**
   * @return the content.
   */
  public String getContent() {
    return content;
  }
  
  /**
   * @param sent whether this Message has been sent.
   */
  public void setSent(boolean sent) {
    this.sent = sent;
  }

  @Override
  public String toString() {
    return "Message{\"+" + getFormatted() + "\"}";
  }

  /**
   * Test whether an Object is equal to a Message.
   * 
   * @param obj the Object to be tested.
   * @return <code>true</code> if <code>this == obj</code> or the formatted Message Strings of both
   *         Messages are equal; <code>false</code> otherwise.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    else if (obj instanceof InternalMessage) {
      InternalMessage m = (InternalMessage) obj;
      if (m.getFormatted().equals(this.getFormatted()))
        return true;
    }
    return false;
  }

  /**
   * @param s formatted Message String to be validated.
   * @return whether the given format is valid.
   */
  private boolean formatIsValid(String s) {
    return (Formats.MESSAGE_FORMAT.matcher(s).matches()
        && headerIsValid(s.split(Character.toString(Formats.DELIMITER_CHAR))[0]) && contentIsValid(s
          .split(Character.toString(Formats.DELIMITER_CHAR))[1]));
  }

  /**
   * @param s Content to be validated.
   * @return whether the given content is valid (shorter or equal than the given message length
   *         limit) and in the right format.
   */
  private boolean contentIsValid(String s) {
    return !(s.length() > Core.getInstance().getSettings().getMsgLenLimit() || s.length() == 0)
        && Formats.MESSAGE_FORMAT_CONTENT.matcher(s).matches();
  }

  /**
   * @param s Header to be validated.
   * @return whether the header is valid (shorter or equal than the given header length limit) and
   *         in the right format.
   */
  private boolean headerIsValid(String s) {
    return !(s.length() > Core.getInstance().getSettings().getHeaderLenLimit() || s.length() < 9)
        && Formats.MESSAGE_FORMAT_HEADER.matcher(s).matches();
  }

  /**
   * Sets the content of a message.
   * 
   * @param text Content to be set.
   * @throws FormatException if the content is invalid
   */
  public void setContent(String text) throws FormatException {
    if (contentIsValid(text)) {
      this.content = text;
    } else {
      throw new FormatException("Content of message is invalid.");
    }
  }

  /**
   * Sets the message via a formatted message string.
   * 
   * @param formattedMsgString Formatted message string to be set.
   * @throws FormatException if the formatted message String is invalid.
   * @see InternalMessage#getFormatted()
   */
  public void setFormatted(String formattedMsgString) throws FormatException {

    if (!formatIsValid(formattedMsgString)) {
      throw new FormatException("Invalid formatted message.");
    } else {
      Matcher m = Formats.MESSAGE_FORMAT.matcher(formattedMsgString);
      timeStamp.setTimeInMillis(Long.parseLong(m.group(1), 16)/1000);
      uuidConversation = m.group(2);
      uuidSender = m.group(3);
      command = m.group(4).charAt(0) == '1';

      content = m.group(5);
    }

  }

  /**
   * Sets the command property of the message.
   * 
   * @param command Boolean to be set.
   */
  public void setCommand(boolean command) {
    this.command = command;
  }

  /**
   * Set the UUID of the Message's sender.
   * 
   * @param uuid UUID to be set.
   */
  @Override
  public void setUuidSender(String uuid) {
    this.uuidSender = uuid;
  }

  @Override
  public int compareTo(InternalMessage anotherInternalMessage) {
    if (timeStamp.equals(anotherInternalMessage.timeStamp))
      return uuidSender.compareTo(anotherInternalMessage.uuidSender);
    else
      return timeStamp.compareTo(anotherInternalMessage.timeStamp);
  }

  @Override
  public EncryptedMessage toEncryptedMessge(Contact forContact) {
    return HybridCoder.encodeMessage(this, forContact);
  }

  @Override
  public InternalMessage toInternalMessage() {
    return this;
  }
}
