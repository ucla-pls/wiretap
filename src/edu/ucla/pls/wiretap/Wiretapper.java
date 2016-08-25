package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;

public abstract class Wiretapper<T> {

  private final Class<T> recorder;

  public Wiretapper (Class<T> recorder) {
    this.recorder = recorder;
  }

  public Class<T> getRecorder() {
    return recorder;
  }

  public abstract Wiretap instrument(MethodVisitor next,
                                     MethodVisitor out,
                                     Method method);

}
