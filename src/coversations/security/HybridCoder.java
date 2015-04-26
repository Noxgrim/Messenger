package coversations.security;

import java.util.Base64;

import javax.crypto.Cipher;

import persons.Contact;
import main.Core;
import misc.Settings;
import exceptions.FormatException;
import exchange.EncryptedMessage;
import exchange.InternalMessage;

public class HybridCoder {

  public static EncryptedMessage encodeMessage(InternalMessage m, Contact forContact) {
    Settings s = Core.getInstance().getSettings();
    
    
    String publicKey = forContact.getPublicKey();
    
    String sessionKey = generateSessionKey(s.getSessionKeyLen(), s.getCharSet());
    

    String contentBase64 = new String(Base64.getDecoder().decode(m.getFormatted().getBytes()));
    

  }

  /**
   * Generates a random session key for the encryption.
   * 
   * @param length The length of the session key. (default: 10)
   * @param charSet the used char set
   * @return returns a session key
   * @see EncryptionCharSets
   * @see Defaults
   */
  private static String generateSessionKey(int length, String charSet) {
    String sK = "";
    int cSLength = charSet.length();

    for (int i = 0; i < length; i++) {
      sK += charSet.charAt(new Double(Math.random() * cSLength).intValue());
    }

    return sK;
  }

  /**
   * Encrypts a message content with a private key method (via the session key).
   * 
   * @param m The message to encrypt.
   * @param sessionKey The session key used.
   * @param charSet The used char set.
   * @return a message with an encrypted content (private key method)
   * @throws FormatException
   * @see EncryptionCharSets
   */
  @SuppressWarnings("unused")
  private static InternalMessage privateEncryption(InternalMessage m, String sessionKey, String charSet)
      throws FormatException {

    String content = m.getContent();
    // TODO Finish!
    m.setContent(content);

    return m;
  }

}
