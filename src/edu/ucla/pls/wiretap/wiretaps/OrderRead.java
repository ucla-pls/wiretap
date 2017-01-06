package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.Field;

public class OrderRead extends Wiretapper {
  EventType preread = declareEventType("preread");
  EventType postread = declareEventType("postread");

	@Override
  public Wiretap createWiretap(MethodVisitor next, RecorderAdapter out) {
    final Emitter preread = this.preread.getEmitter(out);
    final Emitter postread = this.postread.getEmitter(out);
    return new Wiretap(next) {

      @Override
      public void visitInsn(int opcode) {
        if (AALOAD >= opcode && opcode < AALOAD + 8) {
          preread.emit();
          super.visitInsn(opcode);
          postread.emit();
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
        if (!f.isFinal()) {
          preread.emit();
          super.visitFieldInsn(opcode, owner, name, desc);
          postread.emit();
        } else {
          super.visitFieldInsn(opcode, owner, name, desc);
        }
      }
    };
	}

}
