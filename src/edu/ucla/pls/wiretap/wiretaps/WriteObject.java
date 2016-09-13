package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.ValueWiretapper;

public class WriteObject extends ValueWiretapper {

  EventType write = declareEventType("write", Object.class, int.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out,
                               final ValueEmitter value) {
    final Emitter write = this.write.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {


        if (opcode == AASTORE) {
          // Copy value up 2 in stack.  [, I, A -> A, [, I, A
          out.visitInsn(DUP_X2);
          // Consume value -> A, [, I
          value.vObject.consume();
          // Copy the two -> [, I, A, [, I
          out.visitInsn(DUP2_X1);
          // Consume value, leaving 2 in the stack for consuming by 'write'
          write.consume2(getInstruction().getId());
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
            // Copy value up 2 in stack. A -> A
            value.vObject.log();
            write.emit(null, getInstructionId());
            break;
          case PUTFIELD:
            // Copy value up 2 in stack.  A, A -> A, A, A
            out.visitInsn(DUP_X2);
            // Consume value -> A, [, I
            value.vObject.consume();
            // Copy the two -> [, I, A, [, I
            out.visitInsn(DUP2_X1);
            // Consume value, leaving 2 in the stack for consuming by 'write'
            write.consume2(getInstructionId());
          }
        }
        super.visitFieldInsn(opcode, owner, name, desc);

      }
    };
  }
}
