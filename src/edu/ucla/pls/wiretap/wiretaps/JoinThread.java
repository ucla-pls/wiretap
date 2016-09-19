package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;

public class JoinThread extends Wiretapper {

  EventType join = declareEventType("join", Thread.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter join = this.join.getEmitter(out);
    return new Wiretap(next) {

      public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {


        if (name.equals("join") && desc.equals("()V") && owner.equals("java/lang/Thread")) {
          out.dup();

          super.visitMethodInsn(opcode, owner, name, desc, itf);

          // Wait for the join fork and consume
          join.consume(createInstructionId());
        } else {
          super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
      }
    };
  }
}
