package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class ReadObject extends Wiretapper {

  EventType read = declareEventType("read", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out) {
    final Emitter read = this.read.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {

        super.visitInsn(opcode);

        if (opcode == Opcodes.AALOAD) {
          read.log(getInstruction().getId());
        }
      }

      @Override
      public void visitFieldInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc) {

        super.visitFieldInsn(opcode, owner, name, desc);

        if (desc.charAt(0) == 'L' || desc.charAt(0) == '[') {
          switch (opcode) {
          case Opcodes.GETSTATIC:
          case Opcodes.GETFIELD:
            read.log(getInstruction().getId());
          }
        }
      }
    };
  }
}
