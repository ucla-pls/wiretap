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

public class AcquireLock extends Wiretapper {

  EventType aquire = declareEventType("aquire", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               MethodVisitor out) {
    final Emitter aquire = this.aquire.getEmitter(out);
    return new Wiretap(next) {

      public void visitInsn(int opcode) {

        if (opcode == MONITORENTER) {
          out.visitInsn(DUP);
          super.visitInsn(opcode);
          aquire.consume(getInstruction().getId());
        } else {
          super.visitInsn(opcode);
        }

      }

      public void visitCode() {

        super.visitCode();

        // After other instrumentations has run.
        if (getMethod().isSynchronized()) {
          aquire.pushRecorder();
          pushContext();
          aquire.record(getInstruction().getId());
        }
      }
    };
  }
}
