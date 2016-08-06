package edu.ucla.pls.wiretap.wiretaps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.Label;
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

  private final String qualifiedName;

  public Basic(MethodVisitor mv, String qualifiedName) {
    super(BasicRecorder.class, mv);
    this.qualifiedName = qualifiedName;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner,
                              String name, String signature,
                              boolean isInterface) {
    if (owner.equals("java/lang/Thread")) {
      if (name.equals("start")) {

        mv.visitInsn(Opcodes.DUP);

        pushRecorder();
        mv.visitInsn(Opcodes.SWAP);
        record("forkThread", "Ljava/lang/Thread;");

        mv.visitMethodInsn(opcode, owner, name, signature, isInterface);

      } else if (name.equals("join") && signature.equals("()V")) {

        mv.visitInsn(Opcodes.DUP);

        mv.visitMethodInsn(opcode, owner, name, signature, isInterface);

        pushRecorder();
        mv.visitInsn(Opcodes.SWAP);
        record("joinThread", "Ljava/lang/Thread;");
      } else {
        mv.visitMethodInsn(opcode, owner, name, signature, isInterface);
      }
    } else {
      mv.visitMethodInsn(opcode, owner, name, signature, isInterface);
    }
  }

  private final Label
    start = new Label(),
    end = new Label(),
    handle = new Label(),
    handleEnd = new Label();

  @Override
  public void visitCode() {

    pushRecorder();
    mv.visitLdcInsn(qualifiedName);
    record("enterMethod", "Ljava/lang/String;");

    mv.visitTryCatchBlock(start, end, end, null); //"java/lang/Throwable");
    visitLabel(start);

    super.visitCode();
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {

    super.visitMaxs(maxStack, maxLocals);

    mv.visitLabel(end);

    pushRecorder();
    mv.visitLdcInsn(qualifiedName);
    record("exitMethod", "Ljava/lang/String;");

    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable" );
    mv.visitInsn(Opcodes.ATHROW);
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
