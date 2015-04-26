package database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import coversations.Conversation;
import coversations.GuestConversation;
import coversations.HostConversation;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.InternalMessage;
import exchange.Message;
import persons.Contact;
import persons.User;
import main.Core;

/**
 * This class can be provides access to the Messenger's database.
 * The database consists of the following tables:
 * Messages:
 * | id | content | sender_uuid | conversation_id | sent |
 * Contacts:
 * | id | name | uuid | public_key | address |
 * Users:
 * | id | name | uuid | private_key | public_key |
 * Conversations:
 * | id | name | uuid | participants_uuids | host |
 * @author G.
 */
public class Database implements AutoCloseable {
//public
  /**
   * Connects to the database.
   * If exceptions occur they aren't thrown, but reported to the Core by using the printError()
   * method.
   * @throws DBException 
   */
  public Database() throws DBException {
    try {
      Class.forName("org.sqlite.JDBC");
      File dbFile = new File(Core.getInstance().getSettings().getDbLocation());
      if (!dbFile.exists() || dbFile.isDirectory() || !dbFile.canRead() || !dbFile.canWrite())
        Core.getInstance().printError("The database file isn't accessible.",
            new FileNotFoundException("The database file doesn't exist or isn't accessible."),
            true);
      connect(dbFile.getPath());
    } catch (ClassNotFoundException e) {
      throw new DBException("The SQLite database driver isn't accessible.\n", e);
    } catch (SQLException e) {
      throw new DBException("Couldn't connect to the database.\n", e);
    }
    assert_constants();
    check_connection();
  }
  
  public Database(boolean createTables) { //TODO finish this shit
    assert createTables(conn);
  }
  
  public static boolean createTables(Connection conn) {
    try (Statement stmt = conn.createStatement();) {
      createMessagesTable(stmt);
      createContactsTable(stmt);
      createUsersTable(stmt);
      createConversationsTable(stmt);
    } catch (SQLException e) {
      Core.getInstance().printError("Tables couldn't be created, because the connection to the"+
          " database failed.", e, false);
      return false;
    }
    return true;
  }
  
  public boolean addMessage(InternalMessage m) {
    //Messages:
    //| id | content | sender_uuid | conversation_id | sent |
    try (Statement stmt = conn.createStatement();) {
      EscapedString content = new EscapedString(m.getContent());
      EscapedString sender_uuid = new EscapedString(m.getUuidSender());
      int conversation_id = getConversationId(new EscapedString(m.getUuidConversation()));
      boolean sent = m.isSent();
      String sql = "INSERT INTO "+MESSAGES_TABLE + "(content,sender_uuid,conversation_id,sent) "
          + "VALUES ("+content.toQuotedString()+","+sender_uuid.toQuotedString()+","+conversation_id
          + ","+(sent?1:0)+");";
      stmt.executeUpdate(sql);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }
  
  /**
   * 
   * @param numberOfMessages
   * @param onlyUnsent
   * @return
   * 
   * @throws IllegaArgumentException
   *   If numberOfMessages <= 0
   */
  public List<Message> getLastNMessages(int numberOfMessages, boolean onlyUnsent) {
    //Messages:
    //| id | content | sender_uuid | conversation_id | sent |
    if (!(numberOfMessages > 0)) {
      throw new IllegalArgumentException("numberOfMessages for getLastNMessages(int,boolean) "+
                                         "must be greater than 0.");
    }
    try (Statement stmt = conn.createStatement();) {
      String sql;
      if (onlyUnsent)
        sql = "SELECT * FROM (SELECT * FROM "+MESSAGES_TABLE+" WHERE sent = 1 "
            + "ORDER BY id DESC LIMIT " + numberOfMessages+") ORDER BY id ASC;";
      else
        sql = "SELECT * FROM (SELECT * FROM "+MESSAGES_TABLE
            + "ORDER BY id DESC LIMIT " + numberOfMessages+") ORDER BY id ASC;";
      ResultSet rs = stmt.executeQuery(sql);
      LinkedList<Message> results = new LinkedList<Message>();
      while (rs.next()) {
        String content = rs.getString("content");
        String sender_uuid = rs.getString("sender_uuid");
        int conversation_id = rs.getInt("conversation_id");
        int sent_int = rs.getInt("sent");
        assert sent_int == 0 || sent_int == 1: "Fatal error: sent_int must be 0 or 1, but is "
                                               +sent_int;
        boolean sent =  sent_int == 1 ? true : false;
        results.add(new InternalMessage(content, getConversation(conversation_id).getUUID(),
            sender_uuid, sent));
      }
      return results;
    } catch (FormatException|SQLException e) {
      Core.getInstance().printError("Couldn't get messages.", e, false);
      return null;
    }
  }
  
  public List<Message> getLastNMessages(int numberOfMessages) {
    return getLastNMessages(numberOfMessages, false);
  }
  
  public List<Message> getLastNUnsentMessages(int numberOfMessages) {
    return getLastNMessages(numberOfMessages, true);
  }
  
  /**
   * 
   * @param c
   * @return
   * @throws DBException
   *   If the contact's UUID already exists in the database.
   */
  public boolean addContact(Contact c) throws DBException {
    //Contacts:
    //| id | name | uuid | public_key | address |
    EscapedString name = new EscapedString(c.getNickname());
    EscapedString uuid = new EscapedString(c.getUUID());
    EscapedString public_key = new EscapedString(c.getPublicKey());
    Blob address = getSerializedBlob(c.getAddress());
    String sql = "INSERT INTO "+CONTACTS_TABLE+"(name,uuid,public_key,address) VALUES ("
        + name.toQuotedString() + "," + uuid.toQuotedString()+ ","+public_key.toQuotedString()
        + ",?);";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
      pstmt.setBlob(1, address);
      ResultSet rs = pstmt.executeQuery("SELECT id FROM "+CONTACTS_TABLE+" WHERE uuid = "
          + uuid.toQuotedString()+";");
      if (rs.next()) {
        throw new DBException("The UUID already exists within the database.");
      }
      rs.close();
      pstmt.executeUpdate(sql);
    } catch (SQLException e) {
      Core.getInstance().printError("Could not add contact.", e, false);
      return false;
    }
    return true;
  }
  
  public List<Contact> getContacts() {
    //Contacts:
    //| id | name | uuid | public_key | address |
    try (Statement stmt = conn.createStatement();) {
      LinkedList<Contact> results = new LinkedList<Contact>();
      String sql = "SELECT * FROM "+CONTACTS_TABLE+";";
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        String public_key = EscapedString.unescape(rs.getString("public_key"));
        InetSocketAddress address =
            (InetSocketAddress) getObjFromSerializedBlob(rs.getBlob("address"));
        results.add(new Contact(name, uuid, public_key, address));
      }
      return results;
    } catch (SQLException e) {
      Core.getInstance().printError("Could not get contacts.", e, false);
      return null;
    }
  }
  
  public boolean addUser(User u) {
    //Users:
    //| id | name | uuid | private_key | public_key |
    try (Statement stmt = conn.createStatement();) {
      EscapedString name = new EscapedString(u.getNickname());
      EscapedString uuid = new EscapedString(u.getUUID());
      EscapedString private_key = new EscapedString(u.getPrivateKey());
      EscapedString public_key = new EscapedString(u.getPublicKey());
      String sql = "INSERT INTO "+USERS_TABLE + "(name,uuid,private_key,public_key) "
          + "VALUES ("+name.toQuotedString()+","+uuid.toQuotedString()+","
          + private_key.toQuotedString() + ","+ public_key.toQuotedString() +");";
      stmt.executeUpdate(sql);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }
  
  /**
   * 
   * @param c
   * @return
   * @throws DBException
   *   If the UUID of the contact exists in the database.
   */
  public boolean addConversation(Conversation c) throws DBException {
    //Conversations:
    //| id | name | uuid | participants_ids | host |
    EscapedString name = new EscapedString(c.getName());
    EscapedString uuid = new EscapedString(c.getUUID());
    Blob participants_ids = getSerializedBlob((Serializable) c.getParticipantsIds());
    int host = c.isHost() ? 1 : 0;
    String sql = "INSERT INTO "+CONVERSATIONS_TABLE+"(name,uuid,participants_ids,host) VALUES ("
        + name.toQuotedString() + "," + uuid.toQuotedString()+ ",?,"
        + host + ");";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
      pstmt.setBlob(1, participants_ids);
      ResultSet rs = pstmt.executeQuery("SELECT id FROM "+CONVERSATIONS_TABLE+" WHERE uuid = "
          + uuid.toQuotedString()+";");
      if (rs.next()) {
        throw new DBException("The UUID of the conversation already exists within the database.");
      }
      rs.close();
      pstmt.executeUpdate(sql);
    } catch (SQLException e) {
      Core.getInstance().printError("Could not add conversation.", e, false);
      return false;
    }
    return true;
  }
  
  @SuppressWarnings("unchecked")
  public List<Conversation> getConversations() {
  //Conversations:
    //| id | name | uuid | participants_ids | host |
    try (Statement stmt = conn.createStatement();) {
      LinkedList<Conversation> results = new LinkedList<Conversation>();
      String sql = "SELECT * FROM "+CONVERSATIONS_TABLE+";";
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        Object participants_uuids_obj = getObjFromSerializedBlob(rs.getBlob("participants_ids"));
        LinkedList<String> participants_uuids;
        if (participants_uuids_obj instanceof LinkedList<?>) {
          for (Object o : (LinkedList<?>)participants_uuids_obj)
            assert o instanceof String: "Fatal error while reading the database: UUID not a string";
        } else {
          throw new AssertionError("Blob participants_ids does not represent a LinkedList");
        }
        participants_uuids = (LinkedList<String>)participants_uuids_obj;
        boolean host = rs.getInt("host") == 1 ? true : false;
        results.add(host ? new HostConversation(uuid, name, participants_uuids) :
          new GuestConversation(uuid, name, participants_uuids));
      }
      return results;
    } catch (SQLException e) {
      Core.getInstance().printError("Could not get contacts.", e, false);
      return null;
    }
  }
  
  public Conversation getConversation(String convUUID) {
    try (Statement stmt = conn.createStatement();) {
      EscapedString escUUID = new EscapedString(convUUID);
      String sql = "SELECT * FROM "+CONVERSATIONS_TABLE+" WHERE uuid = "
          +escUUID.toQuotedString()+";";
      ResultSet rs = stmt.executeQuery(sql);
      int count = 0;
      while(rs.next()) {
        ++count;
        assert count == 1: "Fatal error: UUID found more than once in database."
                           +"(getConversation(String)"; 
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        @SuppressWarnings("unchecked")
        List<String> participants_ids = (LinkedList<String>)
            getObjFromSerializedBlob(rs.getBlob("participants_ids");
        boolean host = rs.getInt("host") == 1 ? true : false;
        return new Conversation(uuid, name, participants_ids, host);
      }
    } catch (SQLException e) {
      Core.getInstance().printError("Could not get contacts.", e, false);
      return null;
    }
  }
  
  public Conversation getConversation(int convID) {
    try (Statement stmt = conn.createStatement();) {
      String sql = "SELECT * FROM "+CONVERSATIONS_TABLE+" WHERE id = "+convID+";";
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        @SuppressWarnings("unchecked")
        List<String> participants_ids = (LinkedList<String>)
            getObjFromSerializedBlob(rs.getBlob("participants_ids");
        boolean host = rs.getInt("host") == 1 ? true : false;
        return new Conversation(uuid, name, participants_ids, host);
      }
    } catch (SQLException e) {
      Core.getInstance().printError("Could not get contacts.", e, false);
      return null;
    }
  }
  
  @Override
  public void close() {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        Core.getInstance().printError("Error while closing the database.", e, false);
      }
    }
  }
//private
  private void connect(String dbPath) throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
  }
  
  private static void createMessagesTable(Statement stmt) throws SQLException {
    //COLUMNS: | id | content | sender_uuid | conversation_id | sent |
    String sql = "CREATE TABLE "+MESSAGES_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "content TEXT NOT NULL,"
        + "sender_uuid TEXT NOT NULL,"
        + "conversation_id INTEGER NOT NULL,"
        + "sent INTEGER NOT NULL);";
    stmt.executeUpdate(sql);
  }
  
  private static void createContactsTable(Statement stmt) throws SQLException {
    //COLUMS: | id | name | uuid | public_key | address |
    String sql = "CREATE TABLE "+CONTACTS_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "name TEXT NOT NULL,"
        + "uuid TEXT NOT NULL,"
        + "public_key TEXT NOT NULL,"
        + "address BLOB NOT NULL);"; //address contains a serialized InetSocketAddress
    stmt.executeUpdate(sql);
  }
  
  private static void createUsersTable(Statement stmt) throws SQLException {
    //COLUMNS: | id | name | uuid | private_key | public_key |
    String sql = "CREATE TABLE "+CONTACTS_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "name TEXT NOT NULL,"
        + "uuid TEXT NOT NULL,"
        + "private_key TEXT NOT NULL,"
        + "public_key TEXT NOT NULL);";
    stmt.executeUpdate(sql);
  }
  
  private static void createConversationsTable(Statement stmt) throws SQLException {
    //COLUMNS: | id | name | participants_id | host |
    String sql = "CREATE TABLE "+CONTACTS_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "name TEXT NOT NULL,"
        + "uuid TEXT NOT NULL,"
        + "participants_ids TEXT NOT NULL,"
        + "host INTEGER NOT NULL);";
    stmt.executeUpdate(sql);
  }
  
  //used to check whether the constants are valid
  private void assert_constants() {
    assert MESSAGES_TABLE != null && !MESSAGES_TABLE.isEmpty() && !MESSAGES_TABLE.contains("'");
    assert CONTACTS_TABLE != null && !CONTACTS_TABLE.isEmpty() && !CONTACTS_TABLE.contains("'");
    assert USERS_TABLE != null && !USERS_TABLE.isEmpty() && !USERS_TABLE.contains("'");
    assert CONVERSATIONS_TABLE != null && !CONVERSATIONS_TABLE.isEmpty() 
        && !CONVERSATIONS_TABLE.contains("'");
  }
  
  private void check_connection() throws DBException {
    try {
      if (conn == null && conn.isClosed())
        throw new DBException("The database isn't connected.");
    } catch (SQLException e) {
      throw new DBException("The database isn't connected.\n",e);
    }
  }
  
  private Blob getSerializedBlob(Serializable s) {
    Blob b = null;
    try {
      b = conn.createBlob();
    } catch (SQLException e) {
      Core.getInstance().printError("Serializing failed.",e,false);
      return null;
    }
    try (OutputStream os = b.setBinaryStream(1);
        ObjectOutputStream oos = new ObjectOutputStream(os);) {
      oos.writeObject(s);
    } catch (IOException |SQLException e) {
      Core.getInstance().printError("Serializing failed.",e,false);
      return null;
    }
    return b;
  }
  
  private Object getObjFromSerializedBlob(Blob b) {
    try (ObjectInputStream ois = new ObjectInputStream(b.getBinaryStream());) {
      return ois.readObject();
    } catch (IOException|SQLException|ClassNotFoundException e) {
      Core.getInstance().printError("Retrieving serialized object failed.",e,false);
      return null;
    }
  }
  
  private int getConversationId(EscapedString uuid) throws SQLException {
    try (Statement stmt = conn.createStatement();) {
      ResultSet rs = stmt.executeQuery("SELECT id FROM "+CONTACTS_TABLE+" WHERE uuid = "
          + uuid.toQuotedString() + ";");
      if (rs.next()) {
        return rs.getInt("id");
      } else {
        throw new SQLException("UUID does not exist in "+CONTACTS_TABLE);
      }
    }
  }
  
  /**
   * This class stores an escaped SQL-String.
   * I use it to make sure I don't double-escape stuff and I don't use empty strings and they aren't
   * null.
   */
  private static class EscapedString {
    private String escaped;
    /**
     * @param s
     *   The string to be escaped.
     * @throws IllegalArgumentException
     *   If the string is empty or null.
     */
    public EscapedString(String s) {
      if(s == null || s.isEmpty()) {
        throw new IllegalArgumentException("Empty or null String given to EscapedString(String)");
      }
      escaped = s.replace("'", "''");
    }
    @Override
    public String toString() {
      return escaped;
    }
    public String toQuotedString() {
      return "'"+escaped+"'";
    }
    public String getUnescaped() {
      return escaped.replace("''", "'");
    }
    public static String unescape(String s) {
      return s.replace("''", "'");
    }
  }
 
  
  private Connection conn;
  
  private static String MESSAGES_TABLE = "messages";
  private static String CONTACTS_TABLE = "contacts";
  private static String USERS_TABLE = "users";
  private static String CONVERSATIONS_TABLE = "conversations";
}
