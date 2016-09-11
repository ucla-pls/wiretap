package edu.ucla.pls.wiretap.wiretaps;

import java.util.Arrays;

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

    return new Wiretap(next) {

      private int max;
      // We might need a bigger array;
      private int[] used = new int[100];

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

      @Override
      public void visitCode() {
        max = getMethod().getNumberOfArgumentLocals();
        super.visitCode();
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
            out.visitInsn(DUP_X2);              // -> L,I,J,I
            out.visitInsn(POP);                 // -> L,I,J
            out.visitVarInsn(LSTORE, ++max);  // -> L,I
            used[max] = -1;
            out.visitInsn(SWAP);                // -> I,L

            logAll();

            out.visitInsn(SWAP);                // -> L,I
            out.visitVarInsn(LLOAD, max);   // -> L,I,J
            used[max] = -1;
            out.visitInsn(DUP2_X1);             // -> L,J,I,J
            out.visitInsn(POP2);                // -> L,J,I,J
          }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }



			@Override
      public void visitVarInsn(int opcode, int var) {
        if (var > max) {
          max = var;
        }

        if (used[var] == -1) {
          used[var] = ++max;
          used[max] = -1;
        } else if (used[var] == 0) {
          used[var] = var;
        }

        super.visitVarInsn(opcode, used[var]);
      }


      @Override
      public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, max);
      }
		};
	}
}
