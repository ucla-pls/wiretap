package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.Wiretap;

/** The Basic Bug, measures the basic events.
 */
public class Basic extends Wiretap {

  private final String methodName;
  private final String signature;
  private final String qualifiedName;

  public Basic(MethodVisitor mv, String methodName, String signature) {
    super("edu/ucla/pls/wiretap/wiretaps/Basic", mv);
    this.methodName = methodName;
    this.signature = signature;
    this.qualifiedName = methodName + signature;
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
    System.out.println("enter " + method);
  }

  @Override
  public void visitCode() {
    mv.visitLdcInsn(qualifiedName);
    dynamicInvoke("enterMethod", "(Ljava/lang/String;)V");
  }

  public static void exitMethod(String method) {
    System.out.println("exit " + method);
  }

  public void visitInsn(int opcode) {
    switch (opcode) {
    case Opcodes.IRETURN:
    case Opcodes.LRETURN:
    case Opcodes.FRETURN:
    case Opcodes.DRETURN:
    case Opcodes.ARETURN:
    case Opcodes.RETURN:
    case Opcodes.ATHROW:
      mv.visitLdcInsn(qualifiedName);
      dynamicInvoke("exitMethod", "(Ljava/lang/String;)V");
    }
    mv.visitInsn(opcode);
  }
}
