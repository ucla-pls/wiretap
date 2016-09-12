package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.Wiretapper;

public class WriteObject extends Wiretapper {

  EventType write = declareEventType("write", Object.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out) {
    final Emitter write = this.write.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {

        if (opcode == Opcodes.AASTORE) {
          write.log(getInstruction().getId());
        }
        super.visitInsn(opcode);

      }

      @Override
      public void visitFieldInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc) {

        if (desc.charAt(0) == 'L' || desc.charAt(0) == '[') {
          switch (opcode) {
          case PUTSTATIC:
          case PUTFIELD:
            write.log(getInstruction().getId());
          }
        }
        super.visitFieldInsn(opcode, owner, name, desc);

      }
    };
  }
}
