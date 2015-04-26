package userInterface;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import persons.Contact;
import main.Core;

public class GraphicalUserIterface implements UserInterface {

  private Core parent;

  JTextPane tp = new JTextPane();

  public GraphicalUserIterface(Core core) {

    this.parent = core;

    JFrame f = new JFrame("Messagner");
    f.setLayout(null);
    JScrollPane sp = new JScrollPane(tp);
    JTextField tf = new JTextField();

    f.setSize(500, 300);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    f.add(sp);

    f.validate();

    f.setVisible(true);
  }

  @Override
  public void printError(String shortMessage, Throwable t) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printError(Throwable t) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printError(Throwable t, boolean fatal) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printError(String shortMessage, Throwable t, boolean fatal) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printError(String shortMessage) {
    // TODO Auto-generated method stub

  }

  @Override
  public void switchToCoversation(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addConversation(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeConverstaion(Contact contact) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printDebugMessage(String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printSystemMessage(String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void printRawMessage(String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void alertDialog(String message) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean confirmDialog(String message) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean confirmDialog(String message, boolean defaultChoise) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String multipleOptionsDialog(String message, String... options) {
    // TODO Auto-generated method stub
    return null;
  }
}
