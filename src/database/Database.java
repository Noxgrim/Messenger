package database;

import java.io.File;
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

import coversations.Conversation;
import coversations.GuestConversation;
import coversations.HostConversation;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.InternalMessage;
import exchange.Message;
import persons.Contact;
import persons.User;

/**
 * This class can be provides access to the Messenger's database.
 * The database consists of the following tables:
 * Messages:
 * | id | content | sender_id | conversation_id | sent |
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
   * @throws DBException 
   */
  public Database(String dbLocation) throws DBException {
    this(false, dbLocation);
  }
  
  /**
   * Connects to the database.
   * @param createTables
   *   If this is true, the database will be created if the file does not exist yet.
   */
  public Database(boolean createTables, String dbLocation) throws DBException {
    try {
      Class.forName("org.sqlite.JDBC");
      File dbFile = new File(dbLocation);
      if (dbFile.exists() && (dbFile.isDirectory() || !dbFile.canRead() || !dbFile.canWrite()))
        throw new DBException("Database file is directory or not readable/writeable.");
      if (!dbFile.exists()) {
        if (createTables) {
          connect(dbFile.getPath());
          createTables();
        } else {
          throw new DBException("Database file at "+dbFile.getPath()+" doesn't exist.");
        }
      }
      connect(dbFile.getPath());
    } catch (ClassNotFoundException e) {
      throw new DBException("The SQLite database driver isn't accessible.\n", e);
    } catch (SQLException e) {
      throw new DBException("Couldn't connect to the database.\n", e);
    }
    assert_constants();
    check_connection();
  }
  
  /**
   * Add a message to the database.
   * @param m
   *   <code>InternalMessage</code>
   * @throws DBException
   */
  public void addMessage(InternalMessage m) throws DBException {
    //Messages:
    //| id | content | sender_id | conversation_id | sent |
    try (Statement stmt = conn.createStatement();) {
      EscapedString content = new EscapedString(m.getContent());
      int sender_id = getContactId(new EscapedString(m.getUuidSender()));
      int conversation_id = getConversationId(new EscapedString(m.getUuidConversation()));
      boolean sent = m.isSent();
      String sql = "INSERT INTO "+MESSAGES_TABLE + "(content,sender_id,conversation_id,sent) "
          + "VALUES ("+content.toQuotedString()+","+sender_id+","+conversation_id
          + ","+(sent?1:0)+");";
      stmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  /**
   * Get a specific amount of the last messages in the database.
   * @param numberOfMessages
   * @param onlyUnsent
   *   Only return messages which haven't been sent yet.
   * @return
   *   A <code>List</code> of <code>Message</code>s
   * @throws DBException 
   * 
   * @throws IllegaArgumentException
   *   If numberOfMessages <= 0
   */
  public List<Message> getLastNMessages(int numberOfMessages, boolean onlyUnsent) throws DBException {
    //Messages:
    //| id | content | sender_id | conversation_id | sent |
    if (!(numberOfMessages > 0)) {
      throw new IllegalArgumentException("NumberOfMessages for getLastNMessages(int,boolean) "+
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
        int sender_id = rs.getInt("sender_id");
        int conversation_id = rs.getInt("conversation_id");
        int sent_int = rs.getInt("sent");
        assert sent_int == 0 || sent_int == 1: "Fatal error: sent_int must be 0 or 1, but is "
                                               +sent_int;
        boolean sent =  sent_int == 1 ? true : false;
        results.add(new InternalMessage(content, getConversation(conversation_id).getUUID(),
            getContactUuid(sender_id), sent));
      }
      return results;
    } catch (FormatException|SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  /**
   * Get a specific amount of the last messages in the database.
   * @param numberOfMessages
   * @return
   *   A <code>List</code> of <code>Message</code>s
   * @throws DBException 
   * 
   * @throws IllegaArgumentException
   *   If numberOfMessages <= 0
   */
  public List<Message> getLastNMessages(int numberOfMessages) throws DBException {
    return getLastNMessages(numberOfMessages, false);
  }
  
  /**
   * Add a <code>Contact</code> to the database,
   * @param c
   *   <code>Contact</code>
   * @return
   * @throws DBException
   *   If the <code>Contact</code>'s UUID already exists in the database.
   */
  public void addContact(Contact c) throws DBException {
    //Contacts:
    //| id | name | uuid | public_key | address |
    EscapedString name = new EscapedString(c.getNickname());
    EscapedString uuid = new EscapedString(c.getUUID());
    EscapedString public_key = new EscapedString(c.getPublicKey());
    Blob address;
    try {
      address = getSerializedBlob(c.getAddress());
    } catch (SQLException e) {
      throw new DBException("Adding contact failed (failed to serialize address):"+e.getMessage());
    }
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
      throw new DBException("Adding contact failed: "+e.getMessage());
    }
  }
  
  /**
   * Get a list of all <code>Contact</code>s in the database.
   * @return
   *  <code>List</code> of <code>Contact</code>s
   * @throws DBException
   */
  public List<Contact> getContacts() throws DBException {
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
      throw new DBException("Retrieving contacts from database failed:"+e.getMessage());
    }
  }
  
  /**
   * Get a <code>Contact</code> from the database by its UUID,
   * @param uuid
   * @return
   *   <code>Contact</code>
   * @throws DBException
   */
  public Contact getContact(String uuid) throws DBException {
    try (Statement stmt = conn.createStatement();) {
      String sql = "SELECT * FROM "+CONTACTS_TABLE+" where uuid = "
          +new EscapedString(uuid).toQuotedString()+";";
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        String name = EscapedString.unescape(rs.getString("name"));
        String public_key = EscapedString.unescape(rs.getString("public_key"));
        InetSocketAddress address =
            (InetSocketAddress) getObjFromSerializedBlob(rs.getBlob("address"));
        return new Contact(name, uuid, public_key, address);
      } else {
        throw new DBException("Contact not found: "+uuid);
      }
    } catch (SQLException e) {
      throw new DBException("Getting contact failed: "+e.getMessage());
    } 
  }
  
  /**
   * Add a <code>User</code> to the database.
   * @param u
   *   <code>User</code>
   * @return
   * @throws DBException 
   */
  public void addUser(User u) throws DBException {
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
    } catch (SQLException e) {
      throw new DBException("Adding user to database failed: "+e.getMessage());
    }
  }
  
  /**
   * Add <code>Conversation</code> to the database.
   * @param c
   *   <code>Conversation</code>
   * @return
   * @throws DBException
   */
  public void addConversation(Conversation c) throws DBException {
    //Conversations:
    //| id | name | uuid | participants_uuids | host |
    EscapedString name = new EscapedString(c.getName());
    EscapedString uuid = new EscapedString(c.getUUID());
    Blob participants_uuids;
    try {
      participants_uuids = getSerializedBlob((Serializable) c.getParticipantsIds());
    } catch (SQLException e) {
      throw new DBException("Adding conversation failed (serializing participants' ids failed):"
          +e.getMessage());
    }
    int host = c.isHost() ? 1 : 0;
    String sql = "INSERT INTO "+CONVERSATIONS_TABLE+"(name,uuid,participants_uuids,host) VALUES ("
        + name.toQuotedString() + "," + uuid.toQuotedString()+ ",?,"
        + host + ");";
    try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
      pstmt.setBlob(1, participants_uuids);
      ResultSet rs = pstmt.executeQuery("SELECT id FROM "+CONVERSATIONS_TABLE+" WHERE uuid = "
          + uuid.toQuotedString()+";");
      if (rs.next()) {
        throw new DBException("The UUID of the conversation already exists within the database.");
      }
      rs.close();
      pstmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw new DBException("Adding conversation failed: "+e.getMessage());
    }
  }
  
  /**
   * Get all <code>Conversation</code>s in the database.
   * @return
   *   <code>List</code> of <code>Conversation</code>s
   * @throws DBException
   * 
   * Has to suppress "unchecked" because it tries to cast a deserialized Object to a
   * <code>List&lt;String&gt;</code>. (I assert that the object is valid beforehand.)
   */
  @SuppressWarnings("unchecked")
  public List<Conversation> getConversations() throws DBException {
  //Conversations:
    //| id | name | uuid | participants_uuids | host |
    try (Statement stmt = conn.createStatement();) {
      LinkedList<Conversation> results = new LinkedList<Conversation>();
      String sql = "SELECT * FROM "+CONVERSATIONS_TABLE+";";
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next()) {
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        Object participants_uuids_obj = getObjFromSerializedBlob(rs.getBlob("participants_uuids"));
        LinkedList<String> participants_uuids;
        if (participants_uuids_obj instanceof LinkedList<?>) {
          for (Object o : (LinkedList<?>)participants_uuids_obj)
            assert o instanceof String: "Fatal error while reading the database: UUID not a string";
        } else {
          throw new AssertionError("Blob participants_uuids does not represent a LinkedList");
        }
        participants_uuids = (LinkedList<String>)participants_uuids_obj;
        boolean host = rs.getInt("host") == 1 ? true : false;
        results.add(host ? new HostConversation(uuid, name, participants_uuids) :
          new GuestConversation(uuid, name, participants_uuids));
      }
      return results;
    } catch (SQLException e) {
      throw new DBException("Retrieving conversations from the database failed: "+e.getMessage());
    }
  }
  
  /**
   * Get a <code>Conversation</code> from the database by its UUID.
   * @param convUUID
   *   <code>String</code> that contains the UUID
   * @return
   *   <code>Conversation</code>
   * @throws DBException
   */
  public Conversation getConversation(String convUUID) throws DBException {
    try (Statement stmt = conn.createStatement();) {
      EscapedString escUUID = new EscapedString(convUUID);
      String sql = "SELECT * FROM "+CONVERSATIONS_TABLE+" WHERE uuid = "
          +escUUID.toQuotedString()+";";
      ResultSet rs = stmt.executeQuery(sql);
      int count = 0;
      while(rs.next()) {
        ++count;
        assert count == 1: "Fatal error: UUID found more than once in database."
                           +"(getConversation(String))"; 
        String name = EscapedString.unescape(rs.getString("name"));
        String uuid = EscapedString.unescape(rs.getString("uuid"));
        @SuppressWarnings("unchecked")
        List<String> participants_uuids = (LinkedList<String>)
            getObjFromSerializedBlob(rs.getBlob("participants_uuids"));
        List<Contact> participants = new LinkedList<>();
        for (String participant_uuid : participants_uuids) {
            participants.add(getContact(participant_uuid));
        }
        boolean host = rs.getInt("host") == 1 ? true : false;
        if (host) {
          return new HostConversation(participants, uuid, name);
        } else {
          //TODO maybe change this; I just assume that the first ID will be the host
          return new GuestConversation(participants.get(0), uuid, name);
        }
      }
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
    return null;
  }
  
  /**
   * Close the connection to the database.
   * @throws DBException
   */
  @Override
  public void close() throws DBException {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        throw new DBException("Closing the database connection failed: "+e.getMessage());
      }
    }
  }
  
//private
  /** Connect to the database.*/
  private void connect(String dbPath) throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
  }
  
  private static void createMessagesTable(Statement stmt) throws SQLException {
    //COLUMNS: | id | content | sender_id | conversation_id | sent |
    String sql = "CREATE TABLE "+MESSAGES_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "content TEXT NOT NULL,"
        + "sender_id INTEGER NOT NULL,"
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
        + "participants_uuids TEXT NOT NULL,"
        + "host INTEGER NOT NULL);";
    stmt.executeUpdate(sql);
  }
  
  /** used to check whether the constants are valid*/
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
  
  private Blob getSerializedBlob(Serializable s) throws SQLException {
    Blob b = conn.createBlob();
    try (OutputStream os = b.setBinaryStream(1);
        ObjectOutputStream oos = new ObjectOutputStream(os);) {
      oos.writeObject(s);
    } catch (IOException e) {
      throw new SQLException(e.getMessage());
    }
    return b;
  }
  
  private Object getObjFromSerializedBlob(Blob b) throws SQLException {
    try (ObjectInputStream ois = new ObjectInputStream(b.getBinaryStream());) {
      return ois.readObject();
    } catch (IOException|ClassNotFoundException e) {
      throw new SQLException(e.getMessage());
    }
  }
  
  private int getConversationId(EscapedString uuid) throws DBException {
    try {
      return getId(uuid, CONVERSATIONS_TABLE);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  private int getContactId(EscapedString uuid) throws DBException {
    try {
      return getId(uuid, CONTACTS_TABLE);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  private int getId(EscapedString uuid, String table) throws SQLException {
    assert table == MESSAGES_TABLE || table == CONTACTS_TABLE || table == USERS_TABLE || 
        table == CONVERSATIONS_TABLE : "Trying to get ID of "
        +uuid.getUnescaped()+" in a nonexistant table: "+table;
    try (Statement stmt = conn.createStatement();) {
      ResultSet rs = stmt.executeQuery("SELECT id FROM "+table+" WHERE uuid = "
          +uuid.toQuotedString() + ";");
      if (rs.next()) {
        int ret = rs.getInt("id");
        assert !rs.next() : "UUID contained more than once in database: "+uuid.getUnescaped();
        return ret;
      } else {
        throw new SQLException("UUID does not exist in "+table+": "+uuid.getUnescaped());
      }
    }
  }
  
  private String getContactUuid(int id) throws DBException {
    try {
      return getUuid(id, CONTACTS_TABLE);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  private String getConversationUuid(int id) throws DBException {
    try {
      return getUuid(id, CONVERSATIONS_TABLE);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  private String getUuid(int id, String table) throws SQLException {
    assert table == MESSAGES_TABLE || table == CONTACTS_TABLE || table == USERS_TABLE || 
        table == CONVERSATIONS_TABLE : "Trying to get UUID of "
        +id+" in a nonexistant table: "+table;
    try (Statement stmt = conn.createStatement();) {
      ResultSet rs = stmt.executeQuery("SLECT uuid FROM "+table+" WHERE id = "+id+";");
      if (rs.next()) return EscapedString.unescape(rs.getString("uuid"));
      else throw new SQLException("ID "+id+" not found in table "+table);
    }
  }
  
  private Conversation getConversation(int id) throws DBException {
    return getConversation(getConversationUuid(id));
  }
  
  private void createTables() throws SQLException {
    try (Statement stmt = conn.createStatement();) {
      createMessagesTable(stmt);
      createContactsTable(stmt);
      createUsersTable(stmt);
      createConversationsTable(stmt);
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
