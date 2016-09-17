package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
<<<<<<< HEAD
import org.objectweb.asm.commons.GeneratorAdapter;
=======
import org.objectweb.asm.Opcodes;
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.ValueWiretapper;
<<<<<<< HEAD
import edu.ucla.pls.wiretap.managers.Field;
=======
import edu.ucla.pls.wiretap.ValueWiretapper.ValueEmitter;
import edu.ucla.pls.wiretap.Wiretapper.Wiretap;
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive

public class ReadPrimitive extends ValueWiretapper {

  EventType read = declareEventType("read", Object.class, int.class, int.class);
  EventType readarray = declareEventType("readarray", Object.class, int.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
<<<<<<< HEAD
                               final GeneratorAdapter out,
=======
                               final MethodVisitor out,
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
                               final ValueEmitter value) {
    final Emitter read = this.read.getEmitter(out);
    final Emitter readarray = this.readarray.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {

        Emitter emitter = value.getTypedEmitter(opcode, IALOAD);

        if (emitter != null && opcode != AALOAD) {
          // Copy array and index
<<<<<<< HEAD
          out.dup2();

          super.visitInsn(opcode);

          // log it and push it up 2 in the stack;
          emitter.logX2();

=======
          out.visitInsn(DUP2);

          super.visitInsn(opcode);
          // log it and push it up 2 in the stack;
          emitter.logX2();
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
          // Consume the array and index
          readarray.consume2(createInstructionId());
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
<<<<<<< HEAD
          Field f = getField(owner, name, desc);
          // Ignore final fields, they do not contribute to synchronization.
          if (! f.isFinal()) {
            Emitter emitter = value.getTypedEmitter(desc);
            switch (opcode) {

            case GETSTATIC:
              // Log the written value. Ignore everything else on the stack.
              super.visitFieldInsn(opcode, owner, name, desc);
              emitter.log();
              read.emit(null, f.getId(), createInstructionId());
              return;

            case GETFIELD:
              // Copy object on the stack.  Object -> Object, Object
              out.dup();
              // Fetch value -> Object, Value
              super.visitFieldInsn(opcode, owner, name, desc);
              // Consume value Object, Value -> Value, Object
              emitter.logX1();
              // Consume the object
              read.consume(f.getId(), createInstructionId());
              return;

            }
=======
          Emitter emitter = value.getTypedEmitter(desc);
          switch (opcode) {

          case GETSTATIC:
            // Log the written value. Ignore everything else on the stack.
            super.visitFieldInsn(opcode, owner, name, desc);
            emitter.log();
            read.emit(null, getFieldId(owner, name, desc), createInstructionId());
            return;

          case GETFIELD:
            // Copy object on the stack.  Object -> Object, Object
            out.visitInsn(DUP);
            // Fetch value -> Object, Value
            super.visitFieldInsn(opcode, owner, name, desc);
            // Consume value Object, Value -> Value, Object
            emitter.logX1();
            // Consume the object
            read.consume(getFieldId(owner, name, desc), createInstructionId());
            return;

>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
          }
        }

        super.visitFieldInsn(opcode, owner, name, desc);
      }
    };
  }
}
