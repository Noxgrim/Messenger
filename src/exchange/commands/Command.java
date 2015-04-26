package exchange.commands;

/**
 * Represents a Command. All commands should inherit it.<br>
 * <br>
 * <ul>
 * <li>The name of the Command is always lower case and represents the first word of the command.
 * It's final and cannot be changed after the creation.
 * <li>The aliases are short names of the Command. They have the same usage as the name but can be
 * changed afterwards.
 * <li>The usage of a Command is the String, that recommends the default usage of the Command.
 * Required arguments should be surrounded by <code><></code> and optional ones by <code>[]</code>.
 * <li>The description of a Command should be a short sentence describing the function of the
 * Command.
 * <li>The help message is the message that will be displayed if the user performs the
 * <code>'/help'</code> Command.
 * </ul>
 *
 */
public abstract class Command implements Comparable<Command> {

  /** The name of the Command. */
  private String name;
  /** The aliases of the Command. */
  protected String[] aliases;
  /** Description of the Command. */
  protected String description;
  /** The help message. */
  protected String help;
  /** The default usage message of the Command. */
  protected String usage;

  /**
   * Constructs a new {@code Command}.
   * 
   * @param name The name of the Command. Cannot be either {@code null} nor empty or contain spaces.<br>
   *        The name is always the String after the '/' of the user input; e.g. In the Command
   *        String <code>/help exit</code>, <code>'help'</code> is the name of the Command.
   * @param usage The default usage String of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so the default is : <code>"Usage: '" + name + "'"</code>
   * @param description The description String of the Command. Shouldn't be <code>null</code> or
   *        empty.<br>
   *        If so it's the same as <code>usage</code>.
   * @param help The help message of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so it's the same as <code>description</code>.
   * @param aliases The aliases of the Command (Can be <code>null</code>).
   */
  public Command(String name, String usage, String description, String help, String[] aliases) {
    if (name == null || name.isEmpty() || name.indexOf((int) ' ') != -1)
      throw new IllegalArgumentException("Illegal Command name!");
    if (aliases == null)
      aliases = new String[0];
    else
      for (int i = 0; i < aliases.length; i++) {
        if (aliases[i] == null || aliases[i].isEmpty() || aliases[i].indexOf((int) ' ') != -1)
          throw new IllegalArgumentException("Illegal alias!");
        else
          aliases[i] = aliases[i].toLowerCase();
      }

    this.name = name.toLowerCase();
    this.usage = usage;
    this.description = description;
    this.help = help;

    this.aliases = aliases;


  }

  /**
   * Constructs a new {@code Command} with <code>null</code> as aliases.
   * 
   * @param name The name of the Command. Cannot be either {@code null} nor empty or contain spaces.
   *        The name is always the String after the '/' of the user input; e.g. In the Command
   *        String <code>/help exit</code>, <code>'help'</code> is the name of the Command.
   * @param usage The default usage String of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so the default is : <code>"Usage: '" + name + "'"</code>
   * @param description The description String of the Command. Shouldn't be <code>null</code> or
   *        empty.<br>
   *        If so it's the same as <code>usage</code>.
   * @param help The help message of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so it's the same as <code>description</code>.
   */
  public Command(String name, String usage, String description, String help) {
    this(name, usage, description, help, null);
  }

  /**
   * Constructs a new {@code Command} with <code>null</code> as aliases and the default help.
   * 
   * @param name The name of the Command. Cannot be either {@code null} nor empty or contain spaces.
   *        The name is always the String after the '/' of the user input; e.g. In the Command
   *        String <code>/help exit</code>, <code>'help'</code> is the name of the Command.
   * @param usage The default usage String of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so the default is : <code>"Usage: '" + name + "'"</code>
   * @param description The description String of the Command. Shouldn't be <code>null</code> or
   *        empty.<br>
   *        If so it's the same as <code>usage</code>.
   */
  public Command(String name, String usage, String description) {
    this(name, usage, description, null, null);
  }

  /**
   * Constructs a new {@code Command} with <code>null</code> as aliases and the default description
   * and help.
   * 
   * @param name The name of the Command. Cannot be either {@code null} nor empty or contain spaces.
   *        The name is always the String after the '/' of the user input; e.g. In the Command
   *        String <code>/help exit</code>, <code>'help'</code> is the name of the Command.
   * @param usage The default usage String of the Command. Shouldn't be <code>null</code> or empty.<br>
   *        If so the default is : <code>"Usage: '" + name + "'"</code>
   */
  public Command(String name, String usage) {
    this(name, usage, null, null, null);
  }

  /**
   * Constructs a new {@code Command} with <code>null</code> as aliases and the default usage,
   * description and help.
   * 
   * @param name The name of the Command. Cannot be either {@code null} nor empty or contain spaces.
   *        The name is always the String after the '/' of the user input; e.g. In the Command
   *        String <code>/help exit</code>, <code>'help'</code> is the name of the Command.
   */
  public Command(String name) {
    this(name, null, null, null, null);
  }

  /**
   * @return the name of a Command. Always the String behind the leading '/' of a Command Message.
   */
  public String getName() {
    return name;
  }

  /**
   * @return all possible aliases of a Command.
   */
  public String[] getAliases() {
    return aliases;
  }

  /**
   * @return the description of the Command and the usage if the set usage String is
   *         <code>null</code> or empty.
   */
  public String getDescription() {
    if (description == null || description.isEmpty())
      return getUsage();
    return description;
  }

  /**
   * @return the help message of the Command and the description if the set usage String is
   *         <code>null</code> or empty.
   */
  public String getHelp() {
    if (help == null || help.isEmpty())
      return getDescription();
    return help;
  }

  /**
   * @return the usage String and the default usage String (<code>"Usage: '" + name + "'"</code>) if
   *         the set usage String is <code>null</code> or empty.
   */
  public String getUsage() {
    if (usage == null || usage.isEmpty())
      this.usage = "Usage: '" + name + "'";

    return usage;
  }

  /**
   * @return whether this Command has aliases.
   */
  public boolean hasAliases() {
    return aliases != null && aliases.length != 0;
  }

  /**
   * @return the name of the Command.
   */
  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(Command o) {
    return name.compareTo(o.name);
  }

  /**
   * Tests whether the name or one of the aliases of the target Command is equal to the of this one.
   * 
   * @param obj The target Object
   * @return <code>this == obj</code> or the name or one of the aliases of the target Command is
   *         equal to the of this one.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    else if (obj instanceof Command) {
      Command c = (Command) obj;

      if (this.name.equalsIgnoreCase(c.name))
        return true;
      else
        for (String alias : aliases)
          for (String alias2 : c.aliases)
            if (alias.equalsIgnoreCase(alias2))
              return true;
    }
    return false;
  }

  /**
   * Performs the Command.
   * 
   * @param args The arguments of the Command.
   * @return <code>true</code> if the Command performed successfully.
   */
  abstract public boolean perform(String[] args);
}
