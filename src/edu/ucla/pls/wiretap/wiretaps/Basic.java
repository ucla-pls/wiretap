package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.ucla.pls.wiretap.Wiretap;

/** The Basic Bug, measures the basic events.
 */
public class Basic extends Wiretap {

  private static final Map<Thread, BasicInfo> context =
    new HashMap<Thread, BasicInfo>();

  public static BasicInfo getInfo() {
    return getInfo(Thread.currentThread());
  }

  public static BasicInfo getInfo(Thread thread) {
    BasicInfo info = context.get(thread);
    if (info == null) {
      info = new BasicInfo(thread);
      context.put(thread, info);
    }
    return info;
  }

  private final String methodName;
  private final String signature;
  private final String qualifiedName;

  public Basic(MethodVisitor mv, String methodName, String signature) {
    super("edu/ucla/pls/wiretap/wiretaps/Basic", mv);
    this.methodName = methodName;
    this.signature = signature;
    this.qualifiedName = methodName + signature;
  }

  public static void fork(Thread thread) {
    System.err.println("Started thread + " + thread);
    thread.start();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner,
                              String name, String signature,
                              boolean isInterface) {
    if (owner.equals("java/lang/Thread") && name.equals("start")) {
      dynamicInvoke("fork", "(Ljava/lang/Thread;)V");
    } else {
      mv.visitMethodInsn(opcode, owner, name, signature, isInterface);
    }
  }

  public static void enterMethod(String method) {
    getInfo().enterMethod(method);
  }

  @Override
  public void visitCode() {
    mv.visitLdcInsn(qualifiedName);
    dynamicInvoke("enterMethod", "(Ljava/lang/String;)V");
  }

  public static void exitMethod(String method) {
    getInfo().exitMethod(method);
  }

  @Override
  public void visitInsn(int opcode) {
    switch (opcode) {
    case Opcodes.IRETURN:
    case Opcodes.LRETURN:
    case Opcodes.FRETURN:
    case Opcodes.DRETURN:
    case Opcodes.ARETURN:
    case Opcodes.RETURN:
      mv.visitLdcInsn(qualifiedName);
      dynamicInvoke("exitMethod", "(Ljava/lang/String;)V");
    }
    mv.visitInsn(opcode);
  }
}

class BasicInfo {
  private final Thread thread;
  private final LinkedList<String> frames = new LinkedList<String>();

  public BasicInfo(Thread thread) {
    this.thread = thread;
  }

  /** enterMethod should be called as the first thing when entering
      a method.
   */
  public BasicInfo enterMethod(String method) {
    this.frames.push(method);
    return this;
  }

  /** exitMethod should be called as the last thing in a method. The
      method should be the top of the stack. 
   */
  public BasicInfo exitMethod(String method) {
    String topMethod = this.frames.pop();
    assert topMethod.equals(method);
    return this;
  }

  /** recoverFrame should be called when the current frame level is known but
      the frame might be wrong.
   */
  public BasicInfo recoverFrame(String method) {
    this.frames.push(method);
    return this;
  }

}

