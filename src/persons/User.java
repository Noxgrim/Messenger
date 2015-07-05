package persons;

public class User {
  private String publicKey;
  private String privateKey;

  private String nickname;
  private String uuid;

  public User() {
    load();
  }

  public User(String nickname, String uuid, String publicKey, String privateKey) {
    this.nickname = nickname;
    this.uuid = uuid;
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  private void load() {
    if (nickname.lastIndexOf(0) == 1) {
      // TODO Load user's data from Database
    }
  }



  public String getUUID() {
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
}
