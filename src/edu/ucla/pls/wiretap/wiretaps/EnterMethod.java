package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretap;
import edu.ucla.pls.wiretap.Wiretapper;

public class EnterMethod<T> extends Wiretapper<T> {

  public EnterMethod (Class<T> recorder) {
    super(recorder);
  }

  @Override
  public Wiretap instrument(MethodVisitor next,
                            MethodVisitor out,
                            final Method method) {
    return
      new Wiretap(Type.getInternalName(getRecorder()), next, out) {
        @Override
        public void visitCode() {
          visitCode();
          emit("enter", method.getId());
        }
      };
  }
}
