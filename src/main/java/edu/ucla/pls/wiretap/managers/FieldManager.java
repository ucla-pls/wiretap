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

  public synchronized Field getField(String owner, String name, String desc) {
    return getUnmanaged(new Field(0, owner, name, desc, null));
  }

}
