package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class ReleaseLock extends Wiretapper {

  EventType release = declareEventType("release", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter release = this.release.getEmitter(out);
    return new Wiretap(next) {
      private final Label
        start = new Label(),
        end = new Label();

      @Override
      public void visitCode() {
        super.visitCode();

        // Surround the entire method in a try-catch to catch exceptions and
        // release the lock
        if (getMethod().isSynchronized()) {
          out.visitTryCatchBlock(start, end, end, null);
          out.mark(start);
        }
      }

      @Override
      public void visitInsn(int opcode) {

        if (opcode == MONITOREXIT) {
          out.dup();
          super.visitInsn(opcode);
          release.consume(createInstructionId());
        } else if (opcode <= RETURN && opcode >= IRETURN && getMethod().isSynchronized()) {
          // If the method is synchronized return.
          out.pushRecorder();
          out.pushContext();
          release.record(createInstructionId());
          super.visitInsn(opcode);
        } else {
          super.visitInsn(opcode);
        }

      }

      @Override
      public void visitMaxs(int maxStack, int maxLocals) {

        if (getMethod().isSynchronized()) {
          out.mark(end);

          out.pushRecorder();
          out.pushContext();
          release.record(createOffsetlessInstructionId());

          // Re-throw the exception
          out.visitTypeInsn(CHECKCAST, "java/lang/Throwable" );
          out.throwException();
        }
        // Before last instrumentation have been made.
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }
}
