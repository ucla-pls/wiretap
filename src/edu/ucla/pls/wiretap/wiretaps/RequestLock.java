package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class RequestLock extends Wiretapper {

  EventType request = declareEventType("request", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               RecorderAdapter out) {

    final Emitter request = this.request.getEmitter(out);
    return new Wiretap(next) {

      public void visitInsn(int opcode) {

        if (opcode == MONITORENTER) {
          request.log(createInstructionId());
        }

        super.visitInsn(opcode);
      }

      public void visitCode() {

        if (getMethod().isSynchronized()) {
          out.pushRecorder();
          out.pushContext();
          request.record(createOffsetlessInstructionId());
        }

        // Before any instrumentation has been made.
        super.visitCode();
      }
    };
  }
}
