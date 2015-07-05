package coversations.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import main.Core;
import persons.Contact;
import exceptions.FormatException;
import exchange.EncryptedMessage;
import exchange.InternalMessage;

public class HybridCoder {

  private static KeyGenerator kg;
  private static KeyPairGenerator kpg;

  private static KeyFactory kf;
  
  private static Cipher aes, rsa;
  
  static String asy;

  static {
    try {

      kg = KeyGenerator.getInstance("AES");
      kpg = KeyPairGenerator.getInstance("RSA");

      try {
        kg.init(Core.getInstance().getSettings().getSessionKeyLen());
      } catch (NullPointerException e) {
        kg.init(128);
        Core.instance
            .printError(
                "Could initialize KeyGenerator: Settings == null\n Initializing with default value (128).",
                e, false);
      }
      kpg.initialize(1024);


      kf = KeyFactory.getInstance("RSA");
      

      aes = Cipher.getInstance("AES");
      rsa = Cipher.getInstance("RSA");


    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      Core.getInstance().printError("Initialization of KeyGenerators failed!", e, true);
    }
  }

  /**
   * Generates a new asynchronous key pair.<br>
   * <pre>[ private key, public key ]</pre>
   * 
   * @return a asynchronous key pair.
   */
  public static String[] generateKeyPair() {
    KeyPair kp = kpg.genKeyPair();
    return new String[]{keyToString(kp.getPrivate()), keyToString(kp.getPublic())};
  }

  public static EncryptedMessage encodeMessage(InternalMessage m, Contact forContact)
      throws InvalidKeyException {
    SecretKey session = kg.generateKey();


    aes.init(Cipher.ENCRYPT_MODE, session);
    try {
      rsa.init(Cipher.ENCRYPT_MODE, getPublicKeyFromString(forContact.getPublicKey()));
    } catch (InvalidKeySpecException e) {
      throw new InvalidKeyException("Invalid key.", e);
    }


    String content = "", sessionKey = "";
    try {

      content =
          new String(Base64.getEncoder().encode(
              aes.doFinal(Base64.getEncoder().encode(m.getFormatted().getBytes("UTF-8")))));

      sessionKey =
          new String(Base64.getEncoder()
              .encode(rsa.doFinal(keyToString(session).getBytes("UTF-8"))));

    } catch (UnsupportedEncodingException e) {
      Core.instance.printError("Couldn't retrieve secret key!", e, true);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      Core.instance.printError("Couldn't encrypt InternalMessage!", e, true);
    }

    return new EncryptedMessage(sessionKey, content);
  }

  public static InternalMessage decodeMessage(EncryptedMessage m) throws InvalidKeyException, FormatException {
    try {
      rsa.init(Cipher.DECRYPT_MODE,
          getPrivateKeyFromString(Core.instance.getUser().getPrivateKey()));
    } catch (InvalidKeySpecException e) {
      throw new InvalidKeyException("Invalid key.", e);
    }

    String formattedIM = "";

    try {
      aes.init(
          Cipher.DECRYPT_MODE,
          getSecretKeyFromString(new String(
              rsa.doFinal(
                  Base64.getDecoder().decode(
              m.getSessionKey().getBytes("UTF-8"))))));

      formattedIM =
          new String(Base64.getDecoder().decode(
              aes.doFinal(Base64.getDecoder().decode(m.getEncrypted().getBytes("UTF-8")))));

    } catch (UnsupportedEncodingException e) {
      Core.instance.printError("Couldn't retrieve secret key!", e, true);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      Core.instance.printError("Couldn't decrypt EncryptedMessage!", e, true);
    }
    
    return new InternalMessage(formattedIM);

  }

  private static String keyToString(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  private static SecretKey getSecretKeyFromString(String key) {
    byte[] decodedKey = null;
    try {
      decodedKey = Base64.getDecoder().decode(key.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      Core.instance.printError("Couldn't retrieve secret key!", e, true);
    }

    return new SecretKeySpec(decodedKey, "AES");
  }

  private static PublicKey getPublicKeyFromString(String key) throws InvalidKeySpecException {
    byte[] decodeKey = null;
    try {
      decodeKey = Base64.getDecoder().decode(key.getBytes("UTF-8"));
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey);
      return kf.generatePublic(keySpec);

    } catch (UnsupportedEncodingException e) {
      Core.instance.printError("Couldn't retrieve public key!", e, true);
    }

    return null;

  }

  private static PrivateKey getPrivateKeyFromString(String key) throws InvalidKeySpecException {
    byte[] decodeKey = null;
    try {
      decodeKey = Base64.getDecoder().decode(key.getBytes("UTF-8"));
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeKey);
      return kf.generatePrivate(keySpec);

    } catch (UnsupportedEncodingException e) {
      Core.instance.printError("Couldn't retrieve public key!", e, true);
    }

    return null;
  }

}
