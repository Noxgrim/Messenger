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
import java.util.Objects;

import persons.Contact;
import persons.User;
import coversations.Conversation;
import coversations.GuestConversation;
import coversations.HostConversation;
import exceptions.DBException;
import exceptions.FormatException;
import exchange.InternalMessage;
import exchange.Message;

/**
 * This class can be provides access to the Messenger's database.
 * The database consists of the following tables:
 * Messages:
 * | id | content | sender_id | conversation_id | timestamp | sent |
 * Contacts:
 * | id | name | uuid | public_key | address |
 * Users:
 * | id | name | uuid | private_key | public_key |
 * Conversations:
 * | id | name | uuid | participants_uuids | host |
 * (If the Conversation is a HostConversation, participants_uuids contains a list of the 
 * participants' UUIDs and host is 1 (->true). If it is a GuestConversation, it contains the UUID
 * of the host and host is 0 (->false).)
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
    //| id | content | sender_id | conversation_id | timestamp | sent |
    try (Statement stmt = conn.createStatement();) {
      EscapedString content = new EscapedString(m.getContent());
      int sender_id = getContactId(new EscapedString(m.getUuidSender()));
      int conversation_id = getConversationId(new EscapedString(m.getUuidConversation()));
      boolean sent = m.isSent();
      long timestamp = m.getTimeStamp().getTimeInMillis();
      String sql = "INSERT INTO "+MESSAGES_TABLE
          + "(content,sender_id,conversation_id,timestamp,sent) "
          + "VALUES ("+content.toQuotedString()+","+sender_id+","+conversation_id
          + ","+timestamp+","+(sent?1:0)+");";
      stmt.executeUpdate(sql);
    } catch (SQLException e) {
      throw new DBException(e.getMessage());
    }
  }
  
  /**
   * Increment the column 'sent' of a message.
   * @param id
   *   The message's ID.
   * @throws IllegalArgumentException
   *   if id < 0
   */
  public void incrementSent(int id) throws DBException {
    if (id < 0) {
      throw new IllegalArgumentException("id smaller than 0. id = "+id);
    }
    String sql = "UPDATE "+MESSAGES_TABLE+" WHERE id = "+id+" SET id = id + 1;";
    try (Statement stmt = conn.createStatement()) {
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
  public List<Message> getLastNMessages(int maxNumberOfMessages, boolean onlyUnsent) throws DBException {
    return _getLastNMessages(null, null, 0, Long.MAX_VALUE, maxNumberOfMessages, onlyUnsent);    
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
    //TODO Should be integrated in advanced method.
    return getLastNMessages(numberOfMessages, false);
  }
  
  /**
   * Get a specific amount of messages in the database.
   * Only messages with fitting criteria will be returned.
   * 
   * @param conversation 
   *    Specifies the conversation of the messages.<br>
   *    Only messages that are in the given conversation 
   *    will be returned.<br>
   * @param contact 
   *    Specifies the contact that sent the messages.<br>
   *    Only messages that were sent by the given contact
   *    will be returned.<br>
   * @param fromTime
   *    Specifies the start of the time span (in UNIX time)
   *    in which the messages were created.<br>
   * @param toTime
   *    Specifies the end of the time span (in UNIX time) 
   *    in which the messages were created.<br>
   * @param maxNumberOfMessages
   *    Specifies the maximum number of messages that will
   *    be returned.<br>
   * @param onlyUnsent
   *    If <code>true</code>, only messages that aren't 
   *    sent will be returned.
   * 
   * @return 
   *    A <code>List</code> of {@link Message}s that fit
   *    the given criteria.
   *    
   * @throws DBException
   *    If a database exception occurs.
   * @throws IllegalArgumentException
   *    If the values of <code>fromTime</code> is bigger than
   *    <code>toTime</code> or one of the two values is smaller 
   *    than <code>0</code> <b>or</b> the value of
   *    <code>maxNumberOfMessages</code> is equal to or smaller than
   *    <code>0</code>.
   * @throws NullPointerException
   *    If either contact or conv are <code>null</code>.
   */
  public List<Message> getLastNMessages(Conversation conv, Contact contact, long fromTime,
      long toTime, int maxNumberOfMessages, boolean onlyUnsent)
          throws DBException, IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(conv, "conv must not be null");
    Objects.requireNonNull(contact, "contact must not be null");
    return _getLastNMessages(conv, contact, fromTime, toTime, maxNumberOfMessages, onlyUnsent);
  }
  
  /**
   * Get a specific amount of messages in the database.
   * Only messages with fitting criteria will be returned.
   * 
   * @param conversation 
   *    Specifies the conversation of the messages.<br>
   *    Only messages that are in the given conversation 
   *    will be returned.<br>
   * @param fromTime
   *    Specifies the start of the time span (in UNIX time)
   *    in which the messages were created.<br>
   * @param toTime
   *    Specifies the end of the time span (in UNIX time) 
   *    in which the messages were created.<br>
   * @param maxNumberOfMessages
   *    Specifies the maximum number of messages that will
   *    be returned.<br>
   * @param onlyUnsent
   *    If <code>true</code>, only messages that aren't 
   *    sent will be returned.
   * 
   * @return 
   *    A <code>List</code> of {@link Message}s that fit
   *    the given criteria.
   *    
   * @throws DBException
   *    If a database exception occurs.
   * @throws IllegalArgumentException
   *    If the values of <code>fromTime</code> is bigger than
   *    <code>toTime</code> or one of the two values is smaller 
   *    than <code>0</code> <b>or</b> the value of
   *    <code>maxNumberOfMessages</code> is equal to or smaller than
   *    <code>0</code>.
   * @throws NullPointerException
   *    If either contact or conv are <code>null</code>.
   */
  public List<Message> getLastNMessages(Conversation conv, long fromTime,
      long toTime, int maxNumberOfMessages, boolean onlyUnsent)
          throws DBException, IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(conv, "conv must not be null");
    return _getLastNMessages(conv, null, fromTime, toTime, maxNumberOfMessages, onlyUnsent);
  }
  
  /**
   * Get a specific amount of messages in the database.
   * Only messages with fitting criteria will be returned.
   * 
   * @param contact 
   *    Specifies the contact that sent the messages.<br>
   *    Only messages that were sent by the given contact
   *    will be returned.<br>
   * @param fromTime
   *    Specifies the start of the time span (in UNIX time)
   *    in which the messages were created.<br>
   * @param toTime
   *    Specifies the end of the time span (in UNIX time) 
   *    in which the messages were created.<br>
   * @param maxNumberOfMessages
   *    Specifies the maximum number of messages that will
   *    be returned.<br>
   * @param onlyUnsent
   *    If <code>true</code>, only messages that aren't 
   *    sent will be returned.
   * 
   * @return 
   *    A <code>List</code> of {@link Message}s that fit
   *    the given criteria.
   *    
   * @throws DBException
   *    If a database exception occurs.
   * @throws IllegalArgumentException
   *    If the values of <code>fromTime</code> is bigger than
   *    <code>toTime</code> or one of the two values is smaller 
   *    than <code>0</code> <b>or</b> the value of
   *    <code>maxNumberOfMessages</code> is equal to or smaller than
   *    <code>0</code>.
   * @throws NullPointerException
   *    If either contact or conv are <code>null</code>.
   */
  public List<Message> getLastNMessages(Contact contact, long fromTime,
      long toTime, int maxNumberOfMessages, boolean onlyUnsent)
          throws DBException, IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(contact, "contact must not be null");
    return _getLastNMessages(null, contact, fromTime, toTime, maxNumberOfMessages, onlyUnsent);
  }
  
  /**
   * Get a specific amount of messages in the database.
   * Only messages with fitting criteria will be returned.
   * 
   * @param conversation 
   *    Specifies the conversation of the messages.<br>
   *    Only messages that are in the given conversation 
   *    will be returned.<br>
   * @param fromTime
   *    Specifies the start of the time span (in UNIX time)
   *    in which the messages were created.<br>
   * @param maxNumberOfMessages
   *    Specifies the maximum number of messages that will
   *    be returned.<br>
   * @param onlyUnsent
   *    If <code>true</code>, only messages that aren't 
   *    sent will be returned.
   * 
   * @return 
   *    A <code>List</code> of {@link Message}s that fit
   *    the given criteria.
   *    
   * @throws DBException
   *    If a database exception occurs.
   * @throws IllegalArgumentException
   *    If the values of <code>fromTime</code> is bigger than
   *    <code>toTime</code> or one of the two values is smaller 
   *    than <code>0</code> <b>or</b> the value of
   *    <code>maxNumberOfMessages</code> is equal to or smaller than
   *    <code>0</code>.
   * @throws NullPointerException
   *    If either contact or conv are <code>null</code>.
   */
  public List<Message> getLastNMessages(Conversation conv, long fromTime,
      int maxNumberOfMessages, boolean onlyUnsent)
          throws DBException, IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(conv, "conv must not be null");
    return _getLastNMessages(conv, null, fromTime, Long.MAX_VALUE, maxNumberOfMessages, onlyUnsent);
  }
  
  /**
   * Get a specific amount of messages in the database.
   * Only messages with fitting criteria will be returned.
   * 
   * @param contact 
   *    Specifies the contact that sent the messages.<br>
   *    Only messages that were sent by the given contact
   *    will be returned.<br>
   * @param fromTime
   *    Specifies the start of the time span (in UNIX time)
   *    in which the messages were created.<br>
   * @param maxNumberOfMessages
   *    Specifies the maximum number of messages that will
   *    be returned.<br>
   * @param onlyUnsent
   *    If <code>true</code>, only messages that aren't 
   *    sent will be returned.
   * 
   * @return 
   *    A <code>List</code> of {@link Message}s that fit
   *    the given criteria.
   *    
   * @throws DBException
   *    If a database exception occurs.
   * @throws IllegalArgumentException
   *    If the values of <code>fromTime</code> is bigger than
   *    <code>toTime</code> or one of the two values is smaller 
   *    than <code>0</code> <b>or</b> the value of
   *    <code>maxNumberOfMessages</code> is equal to or smaller than
   *    <code>0</code>.
   * @throws NullPointerException
   *    If either contact or conv are <code>null</code>.
   */
  public List<Message> getLastNMessages(Contact contact, long fromTime,
      int maxNumberOfMessages, boolean onlyUnsent)
          throws DBException, IllegalArgumentException, NullPointerException {
    Objects.requireNonNull(contact, "contact must not be null");
    return _getLastNMessages(null, contact, fromTime, Long.MAX_VALUE,
        maxNumberOfMessages, onlyUnsent);
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
   * Edits a contact in the database.
   * 
   * @param contactUuid 
   *    The UUID of the contact to be edited.
   * @param newContact
   *    The new contact information.<br>
   *    The former contact's UUID will not be 
   *    overwritten.
   *    
   * @throws DBException
   *    If a database exception occurs.
   */
  public void editContact(String contactUuid, Contact newContact) throws DBException {
    // TODO Non-auto-generated method stub.
    // The @throws DBException has to be updated.
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
   * Add <code>HostConversation</code> to the database.
   * @param c
   *   <code>HostConversation</code>
   * @return
   * @throws DBException
   */
  public void addConversation(HostConversation c) throws DBException {
    //Conversations:
    //| id | name | uuid | participants_uuids | host |
    EscapedString name = new EscapedString(c.getName());
    EscapedString uuid = new EscapedString(c.getUUID());
    final int host = 1;
    Blob participants_uuids;
    try {
      participants_uuids = getSerializedBlob((Serializable) c.getParticipantsIds());
    } catch (SQLException e) {
      throw new DBException("Adding conversation failed (serializing participants' IDs failed):"
          +e.getMessage());
    }
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
   * Add <code>GuestConversation</code> to the database.
   * @param c
   *   <code>GuestConversation</code>
   * @return
   * @throws DBException
   */
  public void addConversation(GuestConversation c) throws DBException {
  //Conversations:
    //| id | name | uuid | participants_uuids | host |
    EscapedString name = new EscapedString(c.getName());
    EscapedString uuid = new EscapedString(c.getUUID());
    final int host = 0;
    EscapedString host_uuid = new EscapedString(c.getHost().getUUID());
    String sql = "INSERT INTO "+CONVERSATIONS_TABLE+"(name,uuid,participants_uuids,host) VALUES ("
        + name.toQuotedString() + "," + uuid.toQuotedString()+ ","+host_uuid.toQuotedString()+","
        + host + ");";
    try (Statement stmt = conn.prepareStatement(sql);) {
      ResultSet rs = stmt.executeQuery("SELECT id FROM "+CONVERSATIONS_TABLE+" WHERE uuid = "
          + uuid.toQuotedString()+";");
      if (rs.next()) {
        throw new DBException("The UUID of the conversation already exists within the database.");
      }
      rs.close();
      stmt.executeUpdate(sql);
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
        boolean host = rs.getInt("host") == 0 ? false : true;
        if (host) {
          Object participants_uuids_obj = getObjFromSerializedBlob(rs.getBlob("participants_uuids"));
          LinkedList<String> participants_uuids;
          if (participants_uuids_obj instanceof LinkedList<?>) {
            for (Object o : (LinkedList<?>)participants_uuids_obj)
              assert o instanceof String: "Fatal error while reading the database: UUID not a string";
          } else {
            throw new AssertionError("Blob participants_uuids does not represent a LinkedList");
          }
          participants_uuids = (LinkedList<String>)participants_uuids_obj;
          results.add(new HostConversation(uuid, name, participants_uuids));
        } else {
            String host_uuid = EscapedString.unescape(rs.getString("participants_uuids"));
            results.add(new GuestConversation(getContact(host_uuid), uuid, name));
        } //end if(host)
      } //end while
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
        boolean host = rs.getInt("host") == 0 ? false : true;
        if (host) {
          @SuppressWarnings("unchecked")
          List<String> participants_uuids = (LinkedList<String>)
              getObjFromSerializedBlob(rs.getBlob("participants_uuids"));
          List<Contact> participants = new LinkedList<>();
          for (String participant_uuid : participants_uuids) {
              participants.add(getContact(participant_uuid));
          }
          return new HostConversation(participants, uuid, name);
        } else { //if(host)
          String host_uuid = EscapedString.unescape(rs.getString("participants_uuids"));
          return new GuestConversation(getContact(host_uuid), uuid, name);
        }
      } //end while
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
    //COLUMNS: | id | content | sender_id | conversation_id | timestamp | sent |
    String sql = "CREATE TABLE "+MESSAGES_TABLE
        + "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
        + "content TEXT NOT NULL,"
        + "sender_id INTEGER NOT NULL,"
        + "conversation_id INTEGER NOT NULL,"
        + "timestamp INTEGER NOT NULL,"
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
  
  //private Conversation getConversation(int id) throws DBException {
  //  return getConversation(getConversationUuid(id));
  //}
  
  private List<Message> _getLastNMessages(Conversation conversation, Contact contact, long fromTime, 
      long toTime, int maxNumberOfMessages, boolean onlyUnsent) 
          throws IllegalArgumentException, DBException {
    
    //Messages:
    //| id | content | sender_id | conversation_id | timestamp | sent |
    if (!(maxNumberOfMessages > 0)) {
      throw new IllegalArgumentException("NumberOfMessages for getLastNMessages(int,boolean) "+
                                         "must be greater than 0. maxNumberOfMessages = "
                                         +maxNumberOfMessages);
    }
    if (fromTime < 0) {
      throw new IllegalArgumentException("fromTime must be positive. fromTime = "+fromTime);
    }
    if (toTime < 0) {
      throw new IllegalArgumentException("toTime must be positive. toTime = "+toTime);
    }
    if (fromTime > toTime) {
      throw new IllegalArgumentException("fromTime must be smaller than toTime. fromTime = "
                                         +fromTime+" toTime = "+toTime);
    }
    
    try (Statement stmt = conn.createStatement();) {
      StringBuilder sql = new StringBuilder(128);
      StringBuilder condition = new StringBuilder(64);
      condition.append("timestamp >= ").append(fromTime).append(" AND timestamp < ").append(toTime)
          .append(" AND ");
      if (conversation != null) {
        condition.append("conversation_id = ").append(getConversationId(
            new EscapedString(conversation.getUUID()))).append(" AND ");
      }
      if (contact != null) {
        condition.append("contact_id = ").append(getContactId(
            new EscapedString(contact.getUUID()))).append(" AND ");
      }
      if (onlyUnsent) {
        condition.append("sent > 0");
      }
      
      if (condition.substring(condition.length() - " AND ".length(), condition.length()) == " AND ") {
        condition.delete(condition.length() - " AND ".length(), condition.length());
      }
      
      sql.append("SELECT * FROM "+MESSAGES_TABLE+" WHERE ").append(condition).append(" LIMIT ")
          .append(maxNumberOfMessages).append(" ORDER BY timestamp ASC;");
      
      ResultSet rs = stmt.executeQuery(sql.toString());
      LinkedList<Message> results = new LinkedList<Message>();
      
      while (rs.next()) {
        String content = rs.getString("content");
        int sender_id = rs.getInt("sender_id");
        int conversation_id = rs.getInt("conversation_id");
        int sent = rs.getInt("sent");
        int id = rs.getInt("id");
        long timestamp = rs.getLong("timestamp");
        assert sent >= 0: "Fatal error: sent_int must be bigger than 0, but is "+sent;
        results.add(new InternalMessage(content, getConversationUuid(conversation_id),
            getContactUuid(sender_id), timestamp, id, sent));
      }
      
      return results;
    } catch (FormatException|SQLException e) {
      throw new DBException(e.getMessage());
    }
    
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
