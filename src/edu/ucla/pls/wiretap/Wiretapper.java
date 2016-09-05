package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassReader$OffsetHandler;

public abstract class Wiretapper {

  private ClassReader$OffsetHandler offsetHandler;

  public void setOffsetHandler (ClassReader$OffsetHandler offsetHandler) {
    this.offsetHandler = offsetHandler;
  }

  public int getOffset() {
    return offsetHandler.getOffset();
  }

  public abstract Wiretap instrument(MethodVisitor next,
                                     MethodVisitor out,
                                     Class<?> recorder,
                                     Method method);

}
