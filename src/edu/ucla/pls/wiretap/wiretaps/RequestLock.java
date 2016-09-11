package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.InstructionManager;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class RequestLock extends Wiretapper {

  EventType request = declareEventType("request", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               MethodVisitor out) {

    final Emitter request = this.request.getEmitter(out);
    return new Wiretap(next) {

      public void visitInsn(int opcode) {

        if (opcode == MONITORENTER) {
          request.log(getInstruction().getId());
        }

        super.visitInsn(opcode);
      }

      public void visitCode() {

        if (getMethod().isSynchronized()) {
          request.pushRecorder();
          pushContext();
          request.record(getInstruction().getId());
        }

        // Before any instrumentation has been made.
        super.visitCode();
      }
    };
  }
}
