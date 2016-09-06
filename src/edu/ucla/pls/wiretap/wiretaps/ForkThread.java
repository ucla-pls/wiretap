package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class ForkThread extends Wiretapper {

  EventType fork = declareEventType("fork", Thread.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out,
                               final Method method) {
    final Emitter fork = this.fork.getEmitter(out);
    return new Wiretap(next) {

      public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {

        final Instruction inst = instructions.getInstruction(method, getOffset());

        if (name.equals("start") && desc.equals("()V")) {
          fork.log();
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);

      }
    };
  }
}
