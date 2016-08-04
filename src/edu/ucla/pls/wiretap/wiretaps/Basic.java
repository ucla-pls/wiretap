package edu.ucla.pls.wiretap.wiretaps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Wiretap;

/** The Basic Bug, measures the basic events.
 */
public class Basic extends Wiretap<BasicRecorder> {

  private static Map<Thread, BasicRecorder> recorders =
    new ConcurrentHashMap<Thread, BasicRecorder>();

  public static BasicRecorder getRecorder() {
    return getRecorder(Thread.currentThread());
  }

  // This method is NOT thread safe across threads, do not call with
  // a thread that is not your own.
  private static BasicRecorder getRecorder(Thread thread) {
    BasicRecorder recorder = recorders.get(thread);
    if (recorder == null) {
      recorder = new BasicRecorder(Agent.v().getLogger(thread));
      recorders.put(thread, recorder);
      recorder.setup();
    }
    return recorder;
  }

  private final String methodName;
  private final String signature;
  private final String qualifiedName;

  public Basic(MethodVisitor mv, String methodName, String signature) {
    super(BasicRecorder.class, mv);
    this.methodName = methodName;
    this.signature = signature;
    this.qualifiedName = methodName + signature;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner,
                              String name, String signature,
                              boolean isInterface) {
    //if (owner.equals("java/lang/Thread") && name.equals("start")) {
    //  // dynamicInvoke("fork", "(Ljava/lang/Thread;)V");
    //} else {
      mv.visitMethodInsn(opcode, owner, name, signature, isInterface);
    //}
  }

  @Override
  public void visitCode() {
    pushRecorder();
    mv.visitLdcInsn(qualifiedName);
    record("enterMethod", "Ljava/lang/String;");
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
      pushRecorder();
      mv.visitLdcInsn(qualifiedName);
      record("exitMethod", "Ljava/lang/String;");
    }
    mv.visitInsn(opcode);
  }
}
