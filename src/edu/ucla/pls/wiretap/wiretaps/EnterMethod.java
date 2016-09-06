package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class EnterMethod extends Wiretapper {

  EventType enter = declareEventType("enter", int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                            final MethodVisitor out) {
    final Emitter enter = this.enter.getEmitter(out);
    return new Wiretap(next) {
      @Override
      public void visitCode() {
        super.visitCode();
        enter.emit(getMethod().getId());
      }
    };
  }
}
