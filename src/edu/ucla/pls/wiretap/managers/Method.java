package edu.ucla.pls.wiretap.managers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Method extends Managable<String> {
  private final int access;
  private final String owner;
  private final String name;
  private final String desc;
  private final String [] exceptions;

  private final String descriptor;

  public Method (int access,
                 String owner,
                 String name,
                 String desc,
                 String [] exceptions) {
    this.access = access;
    this.owner = owner;
    this.name = name;
    this.desc = desc;
    this.exceptions = exceptions;

    this.descriptor = MethodManager.getMethodDescriptor(owner, name, desc);
  }

  public String getOwner () {
    return owner;
  }

  public Type getOwnerType () {
    return Type.getObjectType(owner);
  }

  public String getName () {
    return this.name;
  }

  public int getAccess () {
    return this.access;
  }

  public String getDesc () {
    return this.desc;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public boolean isSynchronized() {
    return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
  }

  public boolean isStatic() {
    return (access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isConstructor() {
    return this.name.equals("<init>");
  }

  public Type[] getArgumentTypes () {
    return Type.getArgumentTypes(desc);
  }

  public String [] getExceptions () {
    return this.exceptions;
  };


  public int getNumberOfArgumentLocals() {
    return getArgumentTypes().length + (isStatic() ? 1: 0);
  }

}
