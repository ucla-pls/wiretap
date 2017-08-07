
package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.MethodManager;

public class ReturnMethod extends Wiretapper {

  EventType returnMethod =
    declareEventType("returnMethod", Object.class, String.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter returnMethod = this.returnMethod.getEmitter(out);
    final MethodManager methods = this.methods;
    return new Wiretap(next) {

      @Override
      public void visitMethodInsn(int opcode, String owner, String name,
                                  String desc, boolean itf) {
        String m = MethodManager.getMethodDescriptor(owner, name, desc);
        super.visitMethodInsn(opcode, owner, name, desc, itf);

        Type type = Type.getReturnType(desc);

        if (type.getSort() == Type.OBJECT) {
          returnMethod.log(m);
        }
      }

    };
  }
}
