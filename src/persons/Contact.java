package persons;

import java.net.InetSocketAddress;

import exceptions.DBException;
import main.Core;


public class Contact {

  /**
   * The nickname of the contact. Can be any String. Can not be longer than the set NickLenLimit.
   * {@link misc.Settings#getNickLenLimit()}
   */
  private String nickname;

  /** The UUID of the contact. */
  private String uuid;
  /** The public key of this user. */
  private String publicKey;
  /** The address of the Contact. */
  private InetSocketAddress address;

  public Contact(String nickname, String uuid, String publicKey, InetSocketAddress address) {

    setNickname(nickname);
    this.uuid = uuid;
    this.address = address;
    this.publicKey = publicKey;
  }
  
  
  /**
   * Gets a Contact from the Database.
   * @param uuid 
   */
  public static Contact getContact(String uuid) throws DBException {
      return Core.getInstance().getDatabase().getContact(uuid);
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    int nickLimit = Core.getInstance().getSettings().getNickLenLimit();
    this.nickname =
        (nickname.length() <= nickLimit) ? nickname : nickname.substring(0, nickLimit + 1);
  }

  public String getUuid() {
    return uuid;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * @return the address
   */
  public InetSocketAddress getAddress() {
    return address;
  }

  /**
   * @param address the address to set
   */
  public void setAddress(InetSocketAddress address) {
    this.address = address;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    else if (obj instanceof Contact)
      return ((Contact) obj).uuid.equals(uuid);
    return false;
  }
}
