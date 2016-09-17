package edu.ucla.pls.wiretap.managers;

import org.objectweb.asm.Opcodes;

public class Field extends Managable<String>{

  private final int access;
  private final String owner;
  private final String name;
  private final String desc;
  private final Object value;

  private final String descriptor;

  public Field (int access,
                String owner,
                String name,
                String desc,
                Object value) {

    this.access = access;
    this.owner = owner;
    this.name = name;
    this.desc = desc;
    this.value = value;

    this.descriptor = FieldManager.getFieldDescriptor(owner, name, desc);
  }

  public String getOwner() {
    return owner;
  }

  public String getName() {
    return name;
  }

  @Override
  public String getDescriptor() {
    return descriptor;
  }

  public boolean isStatic() {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isFinal() {
    return (access & Opcodes.ACC_FINAL) != 0;
  }

}
