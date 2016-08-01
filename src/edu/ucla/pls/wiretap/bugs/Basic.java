package edu.ucla.pls.wiretap.bugs;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** The Basic Bug, measures the basic events.
 */
public class Basic extends MethodVisitor {

  private final String methodName;
  private final String signature;
  private final String qualifiedName;

  public Basic(MethodVisitor mv, String methodName, String signature) {
    super(Opcodes.ASM5, mv);
    this.methodName = methodName;
    this.signature = signature;
    this.qualifiedName = methodName + signature;
  }

  private void dynamicInvoke(String name, String signature) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                       "edu/ucla/pls/wiretap/bugs/Basic",
                       name, signature, false);
  }

  public void dynamicPrintln(String string) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System",
                      "err","Ljava/io/PrintStream;");
    mv.visitLdcInsn(string);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                       "println", "(Ljava/lang/String;)V");

  }

  public static void startThread(Thread thread) {
    System.err.println("Started thread + " + thread);
    thread.start();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner,
                              String name, String signature,
                              boolean isInterface) {
    if (owner.equals("java/lang/Thread") && name.equals("start")) {
      //dynamicInvoke("startThread", "(Ljava/lang/Thread;)V");
    }
    mv.visitMethodInsn(opcode, owner, name, signature, isInterface);
  }

  public static void enterMethod(String method) {
    System.err.println("enter " + method);
  }

  @Override
  public void visitCode() {
    mv.visitLdcInsn(qualifiedName);
    dynamicInvoke("enterMethod", "(Ljava/lang/String;)V");
  }
}
