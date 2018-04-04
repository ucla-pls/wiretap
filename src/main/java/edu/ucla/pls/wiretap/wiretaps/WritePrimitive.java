package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.ValueWiretapper;
import edu.ucla.pls.wiretap.managers.Field;

public class WritePrimitive extends ValueWiretapper {

  EventType postwrite = declareEventType("postwrite");
  EventType write = declareEventType("write", Object.class, int.class, int.class);
  EventType writearray = declareEventType("writearray", Object.class, int.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out,
                               final ValueEmitter value) {
    final Emitter postwrite = this.postwrite.getEmitter(out);
    final Emitter write = this.write.getEmitter(out);
    final Emitter writearray = this.writearray.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {
        Emitter emitter = value.getTypedEmitter(opcode, IASTORE);

        if (emitter != null && opcode != AASTORE) {
          // Array, Index, Value...
          emitter.logX2();
          // Value..., Array, Index

          if (emitter.getType(0).getSize() == 2) {
            out.visitInsn(DUP2_X2);
          } else {
            out.visitInsn(DUP2_X1);
          }
          // Array, Index, Value..., Array, Index

          // Consume value, leaving 2 in the stack for consuming by 'writearray'
          writearray.consume2(createInstructionId());

          // Array, Index, Value...
          super.visitInsn(opcode);

          postwrite.emit();
        } else {
          super.visitInsn(opcode);
        }

      }

      @Override
      public void visitFieldInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc) {

        if (desc.charAt(0) != 'L' && desc.charAt(0) != '[') {
          Emitter emitter = value.getTypedEmitter(desc);
          Field f = getField(owner, name, desc);
          switch (opcode) {

          case PUTSTATIC:
            // Log the written value. Ignore everything else on the stack.
            emitter.log();
            write.emit(null, f.getId(), createInstructionId());

            super.visitFieldInsn(opcode, owner, name, desc);

            postwrite.emit();
            return;

          case PUTFIELD:
            // Object, Value...
            emitter.logX1();
            // Value..., Object
            if (emitter.getType(0).getSize() == 2) {
              out.dupX2();
            } else {
              out.dupX1();
            }
            // Object, Value..., Object
            write.consume(f.getId(), createInstructionId());
            // Object, Value...

            super.visitFieldInsn(opcode, owner, name, desc);

            postwrite.emit();
            return;

          }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    };

  }
}
