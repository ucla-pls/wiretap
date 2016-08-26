package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretap;
import edu.ucla.pls.wiretap.Wiretapper;

public class EnterMethod extends Wiretapper {

  @Override
  public Wiretap instrument(MethodVisitor next,
                            MethodVisitor out,
                            Class<?> recorder,
                            final Method method) {
    return new Wiretap(Type.getInternalName(recorder), next, out) {
      @Override
      public void visitCode() {
        super.visitCode();
        emit("enter", method.getId());
      }
    };
  }
}
