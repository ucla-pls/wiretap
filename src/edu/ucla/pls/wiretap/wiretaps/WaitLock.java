package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Wiretapper;

public class WaitLock extends Wiretapper {

  EventType request = declareEventType("request", Object.class, int.class);
  EventType acquire = declareEventType("acquire", Object.class, int.class);
  EventType release = declareEventType("release", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               MethodVisitor out) {

    final Emitter request = this.request.getEmitter(out);
    final Emitter acquire = this.acquire.getEmitter(out);
    final Emitter release = this.release.getEmitter(out);

    return new LocalWiretap(next) {

      public void logAll() {
        // This method assumes that the Locking object is on the top of the
        // stack, and will leave it on the the top of the stack after: L -> L
        Integer id = getInstruction().getId();
        out.visitInsn(DUP);
        // Assume that the recorder is the same for all Events.
        request.pushRecorder();
        out.visitInsn(SWAP);
        out.visitInsn(DUP2);
        out.visitInsn(DUP2);
        release.record(id);
        request.record(id);
        acquire.record(id);
      }

			public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {

        if (name.equals("wait") && owner.equals("java/lang/Object")) {

          if (desc.equals("()V")) {
            logAll();
          } else if (desc.equals("(J)V")) {
            out.visitInsn(DUP2_X1);  // L,J -> J,L,J
            out.visitInsn(POP2);     // J,L,J -> J,L

            logAll();

            out.visitInsn(DUP_X2);  // J,L -> L,J,L
            out.visitInsn(POP);     // J,L,J -> J,L
          } else if (desc.equals("(JI)V")) {
            int freeLocal = getFreeLocal();
            out.visitInsn(DUP_X2);              // -> L,I,J,I
            out.visitInsn(POP);                 // -> L,I,J
            out.visitVarInsn(LSTORE, freeLocal);  // -> L,I
            out.visitInsn(SWAP);                // -> I,L

            logAll();

            out.visitInsn(SWAP);                // -> L,I
            out.visitVarInsn(LLOAD, freeLocal);   // -> L,I,J
            out.visitInsn(DUP2_X1);             // -> L,J,I,J
            out.visitInsn(POP2);                // -> L,J,I,J
          }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }

    };
	}
}
