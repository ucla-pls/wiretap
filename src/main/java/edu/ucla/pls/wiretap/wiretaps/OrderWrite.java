package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.Field;

public class OrderWrite extends Wiretapper {
  EventType prewrite = declareEventType("prewrite");
  EventType postwrite = declareEventType("postwrite");

	@Override
  public Wiretap createWiretap(MethodVisitor next, RecorderAdapter out) {
    final Emitter prewrite = this.prewrite.getEmitter(out);
    final Emitter postwrite = this.postwrite.getEmitter(out);
    return new Wiretap(next) {

      @Override
      public void visitInsn(int opcode) {
        if (IASTORE <= opcode && opcode < IASTORE + 8) {
          prewrite.emit();
          super.visitInsn(opcode);
          postwrite.emit();
        } else {
          super.visitInsn(opcode);
        }
      }

      @Override
      public void visitFieldInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc) {
        Field f = getField(owner, name, desc);
        if (!f.isFinal() && (opcode == PUTSTATIC || opcode == PUTFIELD)) {
          prewrite.emit();
          super.visitFieldInsn(opcode, owner, name, desc);
          postwrite.emit();
        } else {
          super.visitFieldInsn(opcode, owner, name, desc);
        }
      }
    };
	}

}
