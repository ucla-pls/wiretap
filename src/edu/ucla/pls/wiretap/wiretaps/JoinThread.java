package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class JoinThread extends Wiretapper {

  EventType join = declareEventType("join", Thread.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out,
                               final Method method) {
    final Emitter join = this.join.getEmitter(out);
    return new Wiretap(next) {

      public void visitMethodInsn(int opcode,
                                  String owner,
                                  String name,
                                  String desc,
                                  boolean itf) {


        if (name.equals("join") && desc.equals("()V")) {
          final Instruction inst = instructions.getInstruction(method, getOffset());

          out.visitInsn(Opcodes.DUP);

          super.visitMethodInsn(opcode, owner, name, desc, itf);

          // Wait for the join fork and consume
          join.consume();
        } else {
          super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
      }
    };
  }
}
