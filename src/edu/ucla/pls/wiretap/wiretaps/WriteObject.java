package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.ValueWiretapper;

public class WriteObject extends ValueWiretapper {

  EventType write = declareEventType("write", Object.class, int.class, int.class);
  EventType writearray = declareEventType("writearray", Object.class, int.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final MethodVisitor out,
                               final ValueEmitter value) {
    final Emitter write = this.write.getEmitter(out);
    final Emitter writearray = this.writearray.getEmitter(out);
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
          writearray.consume2(createInstructionId());
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
            // Log the written value. Ignore everything else on the stack.
            value.vObject.log();
            write.emit(null, getFieldId(owner, name, desc), createInstructionId());
            break;
          case PUTFIELD:
            // Copy value up 1 in stack.  Object, Value -> Value, Object, Value
            out.visitInsn(DUP_X1);
            // Consume value -> Value, Object
            value.vObject.consume();
            // Copy the object -> Object, Value, Object
            out.visitInsn(DUP_X1);
            // Consume object
            write.consume(getFieldId(owner, name, desc), createInstructionId());
            break;
          }
        }
        super.visitFieldInsn(opcode, owner, name, desc);

      }
    };
  }
}
