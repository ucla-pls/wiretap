package edu.ucla.pls.wiretap.managers;

import java.util.List;

import edu.ucla.pls.wiretap.WiretapProperties;

public class FieldManager extends Manager<String, Field> {

  public final WiretapProperties properties;

  public FieldManager (WiretapProperties properties, List<Field> fields) {
    super(properties.getFieldFile(), fields);
    this.properties = properties;
  }

  public FieldManager (WiretapProperties properties) {
    super(properties.getFieldFile());
    this.properties = properties;
  }

  public static String getFieldDescriptor(String owner, String name, String desc) {
    StringBuilder b = new StringBuilder();
    b.append(owner);
    b.append(".");
    b.append(name);
    b.append(":");
    b.append(desc);
    return b.toString();
  }

  public Field getField(String owner, String name, String desc) {
    String descriptor = getFieldDescriptor(owner, name, desc);
    Field f = getUnsafe(descriptor);
    if (f == null) {
      return put(new Field(0, owner, name, desc, null));
    }
    return f;
  }

}
