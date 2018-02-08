package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class Branch extends Wiretapper {

  EventType branch = declareEventType("branch", int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out
                               ) {
    final Emitter branch = this.branch.getEmitter(out);
    return new Wiretap(next) {
      @Override
      public void visitJumpInsn(int opcode, Label label) {
        if (opcode != GOTO)
          branch.emit(createInstructionId());
        super.visitJumpInsn(opcode, label);
      }
    };
  }
}
