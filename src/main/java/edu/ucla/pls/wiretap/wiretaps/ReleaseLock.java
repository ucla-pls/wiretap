package edu.ucla.pls.wiretap.wiretaps;

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
      @Override
      public void visitInsn(int opcode) {
        if (opcode == MONITOREXIT) {
          out.dup();
          release.consume(createInstructionId());
          super.visitInsn(opcode);
        } else {
          super.visitInsn(opcode);
        }
      }
    };
  }
}
