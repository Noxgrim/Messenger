package misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import main.Core;
import exceptions.FormatException;



public class ConfiguarionFile {

  /**
   * The location of the configuration file. Can be set via Command line arguments.
   */
  protected String fileLocation;

  /**
   * Constructs a new configuration file.
   *  
   * @param creationType
   *    Sets the creation type of the configuration file.<br>
   *    Following values are allowed:<br>
   *    <ul>
   *        <li> <code>load</code>: loads an existing file or creates a new one.<br>
   *            Calls {@link #load()}.
   *        <li> <code>default</code>: loads default values in created configuration 
   *            file and <b>doesn't</b> save it.<br>
   *            Calls {@link #setToDefault(boolean)} with <code>false</code> as
   *            argument.
   *        <li> <code>void</code>: creates a configuration file without loading any values.
   *    </ul>
   * @param fileLocation
   *    The location of the file.
   */
  public ConfiguarionFile(String creationType, String fileLocation) {
    this.fileLocation = fileLocation;

    switch (creationType.toLowerCase()) {
      case "load":
        this.load();
        break;
      case "void":
        break;
      case "default":
        this.setToDefault(false);
        break;
      default:
        throw new IllegalArgumentException("\"" + creationType + "\" is not supported");
    }
  }

  /**
   * Set all values of this Configuration file to their default value.<br>
   * The default values are defined by the <code>defaultValue</code> of
   * the <code>@Data</code> annotation.
   * @param save if set to true the changes will be saved.
   */
  public void setToDefault(boolean save) {
  
    for (FieldData fd : FieldData.getSaveableFields(this.getClass()))
      try {
  
        fd.invoke(fd.getData().filePath() ? fd.getData().defaultValue().replace('/', File.separatorChar)
            : fd.getData().defaultValue(), this);
        
      } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | SecurityException e) {
        Core.instance.printError(null, e, false);
      }
  
    if (save) {
      this.save();
    }
  }

  /**
   * Saves the configuration file. If such a file does not exist it will be created.<br>
   * <br>
   * 
   * This method tries to save a field (with {@link Data}-annotation) by invoking the corresponding
   * <code>getter</code>-method. The default getter name will be created with the following syntax:
   * 
   * <pre>
   * 'get' + field name
   * </pre>
   * 
   * If you want to avoid the saving of a field you can use the <code>save</code> value of
   * {@link Data}, If your field has a getter but it has a slightly different name as the default
   * creation syntax you can specify it with the <code>getter</code> value of {@link Data}. This
   * value has to be the exact name of the method (So it will not just replace
   * <code>field name</code> in the creation syntax.) Also, a comment will be saved behind the value
   * if <code>comment</code> is specified.<br>
   * <code>final</code> fields wont be saved.
   * 
   * @see Data
   */
  public void save() {
  
    try (BufferedWriter br = new BufferedWriter(new FileWriter(new File(fileLocation)))) {
  
      // Write the date in the first line
      br.write('#' + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
          Locale.getDefault()).format(new Date()));
  
      for (FieldData fd : FieldData.getSaveableFields(this.getClass())) {
        try {
          String value = this.getClass().getMethod(fd.getGetter()).invoke(this).toString();
  
          br.write('\n' + fd.getSavedSpelling() + '=' + value + fd.getComment());
  
        } catch (Exception e) {
          Core.instance.printError("An Error occurred while saving \"" + fd.getField().getName() + "\"",
              e, false);
        }
      }
  
      br.write("\n");
    } catch (IOException e) {
      Core.instance.printError(null, e, false);
    }
  
  }

  /**
   * Loads the configuration file. If one of the keys is unknown or the value couldn't be parsed the
   * default settings will be loaded. If one of the values in invalid it will be replaced with the
   * corresponding default value. If the file does not exist it will be created and the default
   * values will be loaded.<br>
   * <br>
   * 
   * This method tries to load a field (with {@link Data}-annotation) by invoking the corresponding
   * <code>setter</code>-method. The default setter name will be created with the following syntax:
   * 
   * <pre>
   * 'set' + field name
   * </pre>
   * 
   * If you want to avoid the loading of a field you can use the <code>load</code> value of
   * {@link Data}, If your field has a setter but it has a slightly different name as the default
   * creation syntax you can specify it with the <code>setter</code> value of {@link Data}. This
   * value has to be the exact name of the method (So it will not just replace
   * <code>field name</code> in the creation syntax.)<br>
   * <code>final</code> fields wont be loaded.
   * 
   * @see Data
   */
  public void load() {
  
    this.setToDefault(false);
  
    File file = new File(fileLocation);
    if (!file.exists())
      this.save();
    else
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
  
        String line;
        while ((line = br.readLine()) != null) {
  
          if (line.indexOf('#') != -1)
            line = line.substring(0, line.indexOf('#'));
          if (line.split("=").length == 2) {
            line = line.trim();
  
  
            try {
              FieldData fd = FieldData.getBySavedSpelling(line.split("=")[0], this.getClass());
  
              fd.invoke(line.split("=")[1], this);
  
  
            } catch (NoSuchFieldException | NoSuchMethodException | SecurityException
                | InvocationTargetException | IllegalAccessException e) {
              Core.instance.printError("Couldn't load value:", e, false);
            }
          }
        }
  
      } catch (IOException e) {
        Core.instance.printError(null, e, false);
      }
  }

  /**
   * Validates an integer.
   * 
   * @param i Integer to be validated
   * @param min Minimal allowed value. ({@code >=})
   * @param max Maximal allowed value. ({@code <=})
   * @param ifInvalid Value that will be returned if the integer is invalid.
   * @return The integer itself or the 'ifInvalid' value.
   */
  protected int validateInt(int i, int min, int max, int ifInvalid) {
    if (i >= min && i <= max)
      return i;
    else
      try {
        throw new FormatException("Integer '" + i + "' is too "
            + ((i < min) ? "small (min: " + min : "big (max: " + max) + ").");
      } catch (FormatException e) {
        Core.instance.printError(null, e, false);
      }
  
    return ifInvalid;
  
  }

  protected int validateInt(int i, int ifInvalid, int... possibilities) {
    for (int j : possibilities)
      if (i == j)
        return i;
    try {
      throw new FormatException("Integer '" + i + "' isn't a valid possibility. (Possible values:"
          + Arrays.toString(possibilities) + ")");
    } catch (FormatException e) {
      Core.instance.printError(null, e, false);
    }
    return ifInvalid;
  }

  /**
   * Represent a field that can be saved and/or loaded by a class.<br>
   * Contains all important extra information of this field and is comparable
   * (mostly for sorting purposes).<br>
   * All extra information is defined by the {@link Data} annotation.
   */
  protected static class FieldData implements Comparable<FieldData> {
  
    /** Describing field. */
    private Field field;
  
    /** Name of field's getter. */
    private String getter;
    /** Name of field's setter. */
    private String setter;
    /** Additional comment of this field. */
    private String comment;
  
    /** Whether this field is accessible. */
    private boolean accessible;
    /** Whether this field can be loaded. */
    private boolean loadable;
    /** Whether this field can be saved. */
    private boolean saveable;
  
    /**
     * Constructs a new FieldData with the Data annotation of the field
     * as Data annotation.
     * 
     * @param field The field to use.
     */
    public FieldData(Field field) {
      this(field, field.getDeclaredAnnotation(Data.class));
    }
  
    /**
     * Constructs a new FieldData with a set field and Data annotation.
     * 
     * @param field The field to be used.
     * @param data The Data annotation to be used.
     */
    public FieldData(Field field, Data data) {
      this.field = field;
  
      accessible = !Modifier.isFinal(field.getModifiers()) && data != null;
  
      String fieldName =
          Character.toUpperCase(field.getName().charAt(0))
              + field.getName().substring(1, field.getName().length());
  
      if (data != null) {
        getter = data.getter();
        setter = data.setter();
        comment = ((!data.comment().isEmpty()) ? " #" + data.comment() : "");
  
        if (getter.isEmpty())
          getter = "get" + fieldName;
        if (setter.isEmpty())
          setter = "set" + fieldName;
  
  
        loadable = data.load() && accessible;
        saveable = data.save() && accessible;
      } else {
  
  
        getter = "get" + fieldName;
        setter = "set" + fieldName;
        comment = "";
  
        loadable = accessible;
        saveable = accessible;
      }
    }
  
    /**
     * @return the field of this FieldData.
     */
    public Field getField() {
      return field;
    }
  
    /**
     * @return the getter of this FieldData.
     */
    public String getGetter() {
      return getter;
    }
  
    /**
     * @return the setter of this FieldData.
     */
    public String getSetter() {
      return setter;
    }
  
    /**
     * @return the comment of this FieldData.
     */
    public String getComment() {
      return comment;
    }
  
    /**
     * @return the Data annotation of this FieldData.
     */
    public Data getData() {
      return field.getDeclaredAnnotation(Data.class);
    }
  
    /**
     * @return 
     *  Whether this FiedData can be loaded.<br>
     *  A field can be loaded if it's accessible and the Data annotation 
     *  specifies that it can be loaded
     *  
     * @see FieldData#isAccessible()
     */
    public boolean isLoadable() {
      return loadable;
    }
  
    /**
     * @return 
     *  Whether this FiedData can be saved.<br>
     *  A field can be saved if it's accessible and the Data annotation 
     *  specifies that it can be saved.
     *  
     * @see FieldData#isAccessible()
     */
    public boolean isSaveable() {
      return saveable;
    }
  
    /**
     * @return 
     *  Whether this FiedData is accessible.<br>
     *  A field is accessible if it has a Data annotation and isn't final.
     */
    public boolean isAccessible() {
      return accessible;
    }
  
    /**
     * Compares the names of the two FieldData objects.
     */
    @Override
    public int compareTo(FieldData o) {
      return this.field.getName().compareTo(o.field.getName());
    }
  
    /**
     * Gets the saved spelling of the name of the field.<br>
     * The name of the field is split with each camel case
     * letter and the words are divided by dashes.<br>
     * For example:
     * <pre>
     *  "programStatusWord"
     * </pre>
     * becomes
     * <pre>
     *  "program-status-word"
     * </pre>
     * 
     * @return the saved spelling of this FieldData.
     */
    public String getSavedSpelling() {
      String name = "", fieldName = field.getName();
      for (char c : fieldName.toCharArray())
        if (Character.toUpperCase(c) == c)
          name += ("-" + c).toLowerCase();
        else
          name += c;
      if (name.startsWith("-"))
        name = name.substring(1, name.length());
  
      return name;
    }
  
    /**
     * Tries to set the value of the field to the value in a String.<br>
     * The field <b>has to have</b> a setter method in order to
     * successfully execute.<br>
     *   
     * @param value
     *  Every value inside <code>value</code> will be interpreted as the
     *  needed type.<br>
     *  Supported types: <code>boolean, byte, int, long, {@link BigInteger}, 
     *  float, double, {@link BigDecimal}, {@link String}</code>
     * @param conf the <code>ConfigurationFile</code> the setter will be invoked on.
     * 
     * @throws NoSuchMethodException
     *  If the setter of this FieldData does not exists in the specified object's 
     *  class.
     * @throws IllegalAccessException
     *  thrown by {@link Method#invoke(Object, Object...)}.
     * @throws InvocationTargetException
     *  thrown by {@link Method#invoke(Object, Object...)}.
     * @throws SecurityException
     *  thrown by {@link Method#invoke(Object, Object...)}.
     * @throws IllegalArgumentException
     *  If the type of <code>value</code> is unknown.
     * @throws NumberFormatException
     *  If <code>value</code> couldn't be converted to a number.
     */
    public void invoke(String value, ConfiguarionFile conf) throws NoSuchMethodException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException,
        SecurityException, NumberFormatException {
  
      Class<?> paramType = null;
  
      for (Method m : conf.getClass().getDeclaredMethods())
        if (m.getName().equalsIgnoreCase(this.getSetter()) && m.getParameterTypes().length == 1)
          paramType = m.getParameterTypes()[0];
  
      if (paramType == null)
        throw new NoSuchMethodException("Couldn't find setter (" + this.getSetter() + "): "
            + this.getField().getName());
  
  
  
      Object arg = null;
  
      if (paramType == boolean.class)
        arg = Boolean.parseBoolean(value);
  
      else if (paramType == byte.class)
        arg = Byte.parseByte(value);
      else if (paramType == int.class)
        arg = Integer.parseInt(value);
      else if (paramType == long.class)
        arg = Long.parseLong(value);
      else if (paramType == BigInteger.class)
        arg = new BigInteger(value);
  
      else if (paramType == float.class)
        arg = Float.parseFloat(value);
      else if (paramType == double.class)
        arg = Double.parseDouble(value);
      else if (paramType == BigDecimal.class)
        arg = new BigDecimal(value);
      else if (paramType == String.class)
        arg = value;
  
      else
        throw new IllegalArgumentException("Unknown Argument type (" + paramType.getName()
            + ") for: " + setter + "()");
  
  
      conf.getClass().getDeclaredMethod(setter, paramType).invoke(conf, arg);
  
  
    }
  
    /**
     * Returns a sorted List of FieldData objects of all accessible fields in a class.<br>
     * A field is accessible if it has a Data annotation and isn't final.
     * 
     * @param clazz the class from which the fields are taken.
     * 
     * @return a List of FieldData objects.
     */
    public static List<FieldData> getAccessibleFields(Class<? extends ConfiguarionFile> clazz) {
      ArrayList<FieldData> list = new ArrayList<FieldData>();
  
      for (Field f : clazz.getDeclaredFields()) {
        FieldData fd = new FieldData(f);
        if (fd.isAccessible())
          list.add(fd);
      }
      Collections.sort(list);
      return list;
    }
  
    /**
     * Returns a sorted List of FieldData objects of all fields that can be saved in a class.<br>
     * A field can be saved if it's accessible and the Data annotation specifies that it can be 
     * saved.
     * 
     * @param clazz the class from which the fields are taken.
     * 
     * @return a List of FieldData objects.
     */
    public static List<FieldData> getSaveableFields(Class<? extends ConfiguarionFile> clazz) {
      ArrayList<FieldData> list = new ArrayList<FieldData>();
  
      for (Field f : clazz.getDeclaredFields()) {
        FieldData fd = new FieldData(f);
        if (fd.isSaveable())
          list.add(fd);
      }
      Collections.sort(list);
      return list;
    }
  
    /**
     * Returns a sorted List of FieldData objects of all fields that can be loaded in a class.<br>
     * A field can be loaded if it's accessible and the Data annotation specifies that it can be 
     * loaded.
     * 
     * @param clazz the class from which the fields are taken.
     * 
     * @return a List of FieldData objects.
     */
    public static List<FieldData> getLoadableFields(Class<? extends ConfiguarionFile> clazz) {
      ArrayList<FieldData> list = new ArrayList<FieldData>();
  
      for (Field f : clazz.getDeclaredFields()) {
        FieldData fd = new FieldData(f);
        if (fd.isLoadable())
          list.add(fd);
      }
      Collections.sort(list);
      return list;
    }
  
    /**
     * Searches for a field with the given saved spelling in a class.
     * 
     * @param savedSpelling the saved spelling for which is searched for.
     * @param clazz the class in which is searched for the field.
     * 
     * @return a fitting FieldData object.
     * 
     * @throws NoSuchFieldException
     *  If no field with that saved spelling exists within the given class.
     */
    public static FieldData getBySavedSpelling(String savedSpelling, Class<? extends ConfiguarionFile> clazz)
        throws NoSuchFieldException {
  
      List<FieldData> list = FieldData.getAccessibleFields(clazz);
      for (FieldData fd : list)
        if (fd.getSavedSpelling().equals(savedSpelling))
          return fd;
      throw new NoSuchFieldException("Field with save name '" + savedSpelling + "' is unknown.");
    }
  }

  /**
   * Placed over a field, this annotation will specify whether the field will be loaded or saved and
   * can specify a comment behind the saved value. If the names of the getters and setters varies
   * from the name of the field the <code>getter</code> and <code>setter</code> values can be used
   * to specify them.<br>
   * If <code>setter</code> isn't set but <code>getter</code> is, the <code>load</code>-method will
   * generate it by switching the <code>s</code> with a <code>g</code>.<br>
   * Default values:
   * <ul>
   * <li> <code>load</code>: <code>true</code>
   * <li> <code>save</code>: <code>true</code>
   * <li> <code>filePath</code>: <code>false</code>
   * <li> <code>comment</code>: <code>""</code>
   * <li> <code>getter</code>: <code>""</code>
   * <li> <code>setter</code>: <code>""</code>
   * </ul>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  protected @interface Data {
  
    /** The default value. */
    String defaultValue();
  
    /** Load this field. */
    boolean load() default true;
  
    /** Save this field. */
    boolean save() default true;
  
    /** The name of the setter-method of this field. */
    String setter() default "";
  
    /** The name of the getter-method of this field. */
    String getter() default "";
  
    /**
     * Comment that will be written behind the saved field value.
     */
    String comment() default "";
  
    /**
     * Will be marked as file path. If field is are file path every <code>'/'</code> will be
     * replaced by the default file system file separator.
     */
    boolean filePath() default false;
  }

}
