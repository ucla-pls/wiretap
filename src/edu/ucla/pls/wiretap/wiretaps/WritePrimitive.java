package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
<<<<<<< HEAD
import org.objectweb.asm.commons.GeneratorAdapter;
=======
import org.objectweb.asm.Type;
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive

import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.ValueWiretapper;
<<<<<<< HEAD
import edu.ucla.pls.wiretap.managers.Field;
=======
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive

public class WritePrimitive extends ValueWiretapper {

  EventType write = declareEventType("write", Object.class, int.class, int.class);
  EventType writearray = declareEventType("writearray", Object.class, int.class, int.class);

  @Override
  public Wiretap createWiretap(MethodVisitor next,
<<<<<<< HEAD
                               final GeneratorAdapter out,
=======
                               final MethodVisitor out,
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
                               final ValueEmitter value) {
    final Emitter write = this.write.getEmitter(out);
    final Emitter writearray = this.writearray.getEmitter(out);
    return new Wiretap(next) {

			@Override
      public void visitInsn(int opcode) {
<<<<<<< HEAD
        Emitter emitter = value.getTypedEmitter(opcode, ISTORE);

        if (emitter != null && opcode != AASTORE) {
          // Array, Index, Value...
          emitter.logX2();
          // Value..., Array, Index

          if (emitter.getType(0).getSize() == 2) {
=======
        Emitter emitter = value.getTypedEmitter(opcode, IALOAD);

        if (emitter != null && opcode != AALOAD) {
          // Copy value up 2 in stack.  [, I, V -> A, [, I
          emitter.logX2();
          // Copy the two -> [, I, A, [, I
          Type type = emitter.getType(0);
          if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) {
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
            out.visitInsn(DUP2_X2);
          } else {
            out.visitInsn(DUP2_X1);
          }
<<<<<<< HEAD
          // Array, Index, Value..., Array, Index

          // Consume value, leaving 2 in the stack for consuming by 'writearray'
          writearray.consume2(createInstructionId());

          // Array, Index, Value...
=======
          // Consume value, leaving 2 in the stack for consuming by 'writearray'
          writearray.consume2(createInstructionId());
>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
        }
        super.visitInsn(opcode);

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

            case PUTSTATIC:
              // Log the written value. Ignore everything else on the stack.
              emitter.log();
              write.emit(null, f.getId(), createInstructionId());
              break;

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
              break;

            }
=======
          Emitter emitter = value.getTypedEmitter(desc);
          switch (opcode) {

          case PUTSTATIC:
            // Log the written value. Ignore everything else on the stack.
            emitter.log();
            write.emit(null, getFieldId(owner, name, desc), createInstructionId());
            break;

          case PUTFIELD:
            // Copy value up 1 in stack.  Object, Value -> Value, Object
            emitter.logX1();
            // Copy the object -> Object, Value, Object
            Type type = emitter.getType(0);
            if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) {
              out.visitInsn(DUP_X2);
            } else {
              out.visitInsn(DUP_X1);
            }
            // Consume object
            write.consume(getFieldId(owner, name, desc), createInstructionId());
            break;

>>>>>>> f2fdf19... Add ReadPrimitive and WritePrimitive
          }
        }
        super.visitFieldInsn(opcode, owner, name, desc);

      }
    };

  }
}
