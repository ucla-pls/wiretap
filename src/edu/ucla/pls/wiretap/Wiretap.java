package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class Wiretap<T extends Recorder> extends MethodVisitor {

  private final Class<T> recorder;
  private final String recorderName;
  private final String wiretapName;

  public Wiretap(Class<T> recorder, MethodVisitor mv) {
    super(Opcodes.ASM5, mv);
    this.recorder = recorder;
    this.recorderName = recorder.getCanonicalName().replace('.', '/');
    this.wiretapName = this.getClass().getCanonicalName().replace('.', '/');
  }

  protected void pushRecorder() {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                       wiretapName, "getRecorder",
                       "()L" + recorderName + ";", false);
  }

  protected void record(String name) {
    record(name, "");
  }

  protected void record(String name, String args) {
    String signature = String.format("(%s)V", args);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                       recorderName, name, signature, false);
  }

  protected void dynamicInvoke(String name, String signature) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, recorderName, name, signature, false);
  }

  protected void dynamicPrintln(String string) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System",
                      "err","Ljava/io/PrintStream;");
    mv.visitLdcInsn(string);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                       "println", "(Ljava/lang/String;)V", false);

  }

}
