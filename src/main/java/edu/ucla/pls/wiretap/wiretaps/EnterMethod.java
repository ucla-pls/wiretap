package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.managers.Method;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class EnterMethod extends Wiretapper {

  EventType enter = declareEventType("enter", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter enter = this.enter.getEmitter(out);
    return new Wiretap(next) {
      @Override
      public void visitCode() {
        Method m = getMethod();
        super.visitCode();
        if (m.isConstructor() || m.isStatic()) {
          out.visitInsn(ACONST_NULL);
        } else {
          out.visitVarInsn(ALOAD, 0);
        }
        enter.consume(getMethod().getId());
      }
    };
  }
}
