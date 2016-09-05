package edu.ucla.pls.wiretap.wiretaps;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.Wiretap;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.Instruction;
import edu.ucla.pls.wiretap.InstructionManager;
import edu.ucla.pls.wiretap.Method;

public class ReadObject extends Wiretapper {

  @Override
  public Wiretap instrument(MethodVisitor next,
                            MethodVisitor out,
                            Class<?> recorder,
                            final Method method) {
    return new Wiretap(Type.getInternalName(recorder), next, out) {

			@Override
      public void visitInsn(int opcode) {
        final Instruction inst = instructions.getInstruction(method, getOffset());


        super.visitInsn(opcode);

        switch (opcode) {
        case Opcodes.AALOAD:
          readObject(inst);
        }
      }

      @Override
      public void visitFieldInsn(int opcode,
                                 String owner,
                                 String name,
                                 String desc) {
        final Instruction inst = instructions.getInstruction(method, getOffset());

        super.visitFieldInsn(opcode, owner, name, desc);

        if (desc.charAt(0) == 'L') {
          switch (opcode) {
          case Opcodes.GETSTATIC:
          case Opcodes.GETFIELD:
            readObject(inst);
          }
        }
      }

      private void readObject(Instruction inst) {
        out.visitInsn(Opcodes.DUP);
        pushRecorder();
        out.visitInsn(Opcodes.SWAP);
        push(inst.getId());
        record("read", "Ljava/lang/Object;", "I");
      }
    };
  }
}
