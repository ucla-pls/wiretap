package edu.ucla.pls.wiretap.wiretaps;

import java.io.InputStream;
import java.io.IOException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.ClassSkimmer;
import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.managers.Method;

public class ReturnMethod extends Wiretapper {

  EventType returnMethod =
    declareEventType("returnMethod", Object.class, String.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter returnMethod = this.returnMethod.getEmitter(out);
    final MethodManager methods = this.methods;
    final WiretapProperties properties = Agent.v().getProperties();
    return new Wiretap(next) {

      @Override
      public void visitMethodInsn(int opcode, String owner, String name,
                                  String desc, boolean itf) {

        super.visitMethodInsn(opcode, owner, name, desc, itf);

        // If the return type is not an object skip this
        if (Type.getReturnType(desc).getSort() != Type.OBJECT) {
          return;
        }

        String m = MethodManager.getMethodDescriptor(owner, name, desc);

        if (properties.isClassIgnored(owner)) {
            returnMethod.log(m);
        }
      }
    };
  }

}
