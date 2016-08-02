package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class Wiretap extends MethodVisitor {

  private final String className;

  public Wiretap(String className, MethodVisitor mv) {
    super(Opcodes.ASM5, mv);
    this.className = className;
  }

  protected void dynamicInvoke(String name, String signature) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, name, signature, false);
  }

  protected void dynamicPrintln(String string) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System",
                      "err","Ljava/io/PrintStream;");
    mv.visitLdcInsn(string);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                       "println", "(Ljava/lang/String;)V", false);

  }

}
