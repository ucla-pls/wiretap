package edu.ucla.pls.wiretap;

public class Method {
  private final int id;
  private final int access;
  private final String className;
  private final String name;
  private final String typeDesc;
  private final String [] exceptions;

  public Method (int id,
                 int access,
                 String className,
                 String name,
                 String typeDesc,
                 String [] exceptions) {
    this.id = id;
    this.access = access;
    this.className = className;
    this.name = name;
    this.typeDesc = typeDesc;
    this.exceptions = exceptions;
  }

  private String descriptor;
  public String getDescriptor() {
    if (descriptor == null) {
      descriptor = MethodHandler.getMethodDescriptor(className, name, typeDesc);
    }
    return descriptor;
  }

  @Override
  public int hashCode() {
    return getDescriptor().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Method)  {
      return ((Method)obj).getDescriptor().equals(getDescriptor());
    }
    return false;
  }

}
