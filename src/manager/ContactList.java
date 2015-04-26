package manager;

import java.util.ArrayList;
import java.util.List;

import persons.Contact;
import main.Core;

public class ContactList {
  private List<Contact> contacts = new ArrayList<Contact>();
  
  public ContactList() {
    loadFromDatabase();
  }
  
  public void loadFromDatabase() {
    
  }
  
  public Contact getByUUID(String uuid) {
    for (Contact c : contacts)
      if (c.getUUID().equals(uuid))
        return c;
    Core.getInstance().getUserInterface().printError("Could not find Contact with UUID '" + uuid + "'.");
    return null;
  }
  
  public void addContact(Contact c) {
    contacts.add(c);
    // TODO Add to database.
  }

}
