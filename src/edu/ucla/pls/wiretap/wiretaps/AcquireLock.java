package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class AcquireLock extends Wiretapper {

  EventType acquire = declareEventType("acquire", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               RecorderAdapter out) {
    final Emitter acquire = this.acquire.getEmitter(out);
    return new Wiretap(next) {

      public void visitInsn(int opcode) {

        if (opcode == MONITORENTER) {
          out.dup();
          super.visitInsn(opcode);
          acquire.consume(createInstructionId());
        } else {
          super.visitInsn(opcode);
        }

      }

      public void visitCode() {

        super.visitCode();

        // After other instrumentations has run.
        if (getMethod().isSynchronized()) {
          out.pushRecorder();
          pushContext();
          acquire.record(createInstructionId());
        }
      }
    };
  }
}
