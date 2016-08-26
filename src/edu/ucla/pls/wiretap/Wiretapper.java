package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;

public abstract class Wiretapper {

  public abstract Wiretap instrument(MethodVisitor next,
                                     MethodVisitor out,
                                     Class<?> recorder,
                                     Method method);

}
