package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class WaitLock extends Wiretapper {

  EventType request = declareEventType("request", Object.class, int.class);
  EventType acquire = declareEventType("acquire", Object.class, int.class);
  EventType release = declareEventType("release", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               RecorderAdapter out) {

    final Emitter request = this.request.getEmitter(out);
    final Emitter acquire = this.acquire.getEmitter(out);
    final Emitter release = this.release.getEmitter(out);

    return new Wiretap(next) {

      public void logAll() {
        // This method assumes that the Locking object is on the top of the
        // stack, and will leave it on the the top of the stack after: L -> L
        Integer id = createInstructionId();
        out.dup();
        // Assume that the recorder is the same for all Events.
        out.pushRecorder();
        out.swap();
        out.dup2();
        release.record(id);
        out.dup2();
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
            out.swap(Type.LONG_TYPE, Type.INT_TYPE);
            logAll();
            out.swap(Type.INT_TYPE, Type.LONG_TYPE);

          } else if (desc.equals("(JI)V")) {
            int freeLocal = out.newLocal(Type.LONG_TYPE);
            // Lock, Long, Long2, Int
            out.swap(Type.LONG_TYPE, Type.INT_TYPE);
            out.storeLocal(freeLocal);
            out.swap();
            // Int, Lock

            logAll();

            out.swap();
            out.loadLocal(freeLocal);
            out.swap(Type.INT_TYPE, Type.LONG_TYPE);
          }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }

    };
	}
}
