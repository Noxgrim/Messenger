package manager;

import java.util.ArrayList;
import java.util.List;

import exceptions.DBException;
import persons.Contact;
import main.Core;

public class ContactList {
  private List<Contact> contacts = new ArrayList<Contact>();
  
  public ContactList() throws DBException {
    loadFromDatabase();
  }
  
  public void loadFromDatabase() throws DBException {
    contacts = Core.getInstance().getDatabase().getContacts();
  }
  
  public void addContact(Contact c) throws DBException {
    contacts.add(c);
    Core.getInstance().getDatabase().addContact(c);
  }
  
  public Contact getContact(String uuid) throws DBException {
    for (Contact c : contacts)
      if (c.getUUID().equals(uuid))
        return c;
    return Core.getInstance().getDatabase().getContact(uuid);
  }

}
