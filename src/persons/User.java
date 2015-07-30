package persons;

import java.lang.reflect.Field;
import java.util.UUID;

import main.Core;
import misc.ConfiguarionFile;
import utils.HybridCoder;
import exceptions.FormatException;

public class User extends ConfiguarionFile {
  
  @Data(defaultValue = "")
  private String publicKey = null;
  @Data(defaultValue = "")
  private String privateKey = null;

  @Data(defaultValue = "MissingNo")
  private String nickname = null;
  @Data(defaultValue = "")
  private String uuid = null;

  public User() {
    super("load", "data/user.conf");
  }
  
  @Override
  public void setToDefault(boolean save) {
    super.setToDefault(save);
    
    Class<? extends User> clazz = this.getClass();
    
    try {
      
      String[] keys = HybridCoder.generateKeyPair();
      
      Field field = clazz.getDeclaredField("publicKey");
      field.set(this, keys[1]);
      
      field = clazz.getDeclaredField("privateKey");
      field.set(this, keys[0]);
      
      field = clazz.getDeclaredField("uuid");
      field.set(this, UUID.randomUUID().toString());
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    
    if (save)
      save();
  }



  public String getUuid() {
    return uuid;
  }

  public String getNickname() {
    return nickname;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPublicKey() {
    return publicKey;
  }
  
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
  
  public void setNickname(String nickname) {
    this.nickname = validateNick(nickname, 1, 
        Core.instance.getSettings().getNickLenLimit());
  }
  
  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }
  
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * Validates a nickname.
   * 
   * @param nick The nickname to be validated.
   * @param minLen The minimal allowed length of the nickname in characters. (>=)
   * @param maxLen The maximal allowed length of the nickname in characters. (>=)
   * @return the nickname itself if it's valid, {@code "MissingNo"} if the name is too short and a
   *         shortened name if the name is too long.
   */
  private String validateNick(String nick, int minLen, int maxLen) {
  
    if (nick.length() >= minLen && nick.length() <= maxLen) {
      return nick;
    }
    if (nick.length() < minLen) {
      try {
        throw new FormatException("Nickname too short. (" + minLen + " character" + 
      ((maxLen == 1) ? "" : "s") + " minimum.)");
      } catch (FormatException e) {
        Core.instance.printError(null, e, false);
      }
      return "MissingNo";
    }
  
    try {
      throw new FormatException("Nickname too long. (" + maxLen + " character"
          + ((maxLen == 1) ? "" : "s") + " maximum.)");
    } catch (FormatException e) {
      Core.instance.printError(null, e, false);
    }
    return nick.substring(0, maxLen);
  
  }
}
