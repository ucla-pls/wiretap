package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class CallMethod extends Wiretapper {

  EventType before = declareEventType("beforeCall", int.class);
  EventType after = declareEventType("afterCall", int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter before = this.before.getEmitter(out);
    final Emitter after = this.after.getEmitter(out);
    return new Wiretap(next) {
      public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {

        final Label start = new Label(), error = new Label(), end = new Label();
        out.visitTryCatchBlock(start, error, error, null);
        out.visitLabel(start);
        before.emit(createInstructionId());
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        after.emit(createInstructionId());
        out.visitJumpInsn(Opcodes.GOTO, end);
        out.visitLabel(error);
        after.emit(createInstructionId());
        out.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable" );
        out.throwException();
        out.visitLabel(end);
      }
    };
  }
}
