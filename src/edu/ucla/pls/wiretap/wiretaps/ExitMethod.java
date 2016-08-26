package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretap;
import edu.ucla.pls.wiretap.Wiretapper;

public class ExitMethod extends Wiretapper {

  @Override
  public Wiretap instrument(MethodVisitor next,
                            MethodVisitor out,
                            Class<?> recorder,
                            final Method method) {
    return new Wiretap(Type.getInternalName(recorder), next, out) {
      private final Label
        start = new Label(),
        end = new Label();

      @Override
      public void visitCode() {
        out.visitTryCatchBlock(start, end, end, null);
        out.visitLabel(start);
        super.visitCode();
      }

      @Override
      public void visitMaxs(int mStack, int mLocals) {
        out.visitLabel(end);
        emit("exit", method.getId());

        // Rethrow the exception
        out.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable" );
        out.visitInsn(Opcodes.ATHROW);

        super.visitMaxs(mStack, mLocals);
      }
    };
  }
}
