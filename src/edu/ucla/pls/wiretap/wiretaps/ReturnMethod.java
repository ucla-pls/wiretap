
package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.managers.Method;

public class ReturnMethod extends Wiretapper {

  EventType returnMethod =
    declareEventType("returnMethod", Object.class, int.class);

  static final String[] LIBRARY_PREFIXES = new String [] {
    "java.",
    "sun.",
    "javax.",
    "com.sun.",
    "com.ibm.",
    "org.xml.",
    "org.w3c.",
    "apple.awt.",
    "com.apple."
  };

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


        Method method = methods.getUnsafe(m);
        if (Type.getReturnType(desc).getSort() != Type.OBJECT) {
        } else if (method == null) {
          System.err.println("Method '" + m + "' not loaded..");
        } else if (method.isNative()) {
          returnMethod.log(method.getId());
        } else {
          for (int i = LIBRARY_PREFIXES.length - 1; i != 0; --i) {
            if (owner.startsWith(LIBRARY_PREFIXES[i])) {
              returnMethod.log(method.getId());
              break;
            }
          }
        }
      }
    };
  }
}
