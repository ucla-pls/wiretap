package edu.ucla.pls.wiretap;

import org.objectweb.asm.ClassReader$OffsetHandler;
import org.objectweb.asm.MethodVisitor;

public abstract class Wiretapper {

  private ClassReader$OffsetHandler offsetHandler;

  protected final InstructionManager instructions = Agent.v().getInstructionManager();
  protected final MethodManager methods = Agent.v().getMethodManager();

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
