package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class ForkThread extends Wiretapper {

  EventType fork = declareEventType("fork", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter fork = this.fork.getEmitter(out);
    return new Wiretap(next) {

      public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {

        if (name.equals("start") && desc.equals("()V") ) {
          System.err.println("WARNING: Assumes that " + owner + " is inheriting from thread.");
          fork.log(createInstructionId());
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);

      }
    };
  }
}
