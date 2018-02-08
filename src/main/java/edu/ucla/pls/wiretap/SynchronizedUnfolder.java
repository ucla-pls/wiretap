package edu.ucla.pls.wiretap;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import edu.ucla.pls.wiretap.managers.Method;

/**
   The Synchronized unfolder takes a synchronized method and inlines all
   of the methods.

 */
public class SynchronizedUnfolder extends GeneratorAdapter {

  private final Method method;
  private final int version;

  public SynchronizedUnfolder(int version,
                              MethodVisitor mv,
                              int access,
                              Method m) {
    super(ASM5, mv, access, m.getName(), m.getDesc());
    this.method = m;
    this.version = version;
  }

  private final Label
    lStart = new Label(),
    lEnd = new Label();

  public void visitCode () {
    pushContext();
    monitorEnter();

    mark(lStart);
    super.visitCode();
  }

  public void visitInsn(int opcode) {
    // Before every return.. exit the monitor
    if (IRETURN <= opcode && opcode < IRETURN + 6) {
      pushContext();
      monitorExit();
    }
    super.visitInsn(opcode);
  }

  public void visitMaxs(int i, int b) {
    super.visitMaxs(i, b);
    mark(lEnd);
    // Before every return.. and if anything goes wrong, exit the monitor and
    // re-throw the exeception.
    catchException(lStart, lEnd, null);
    pushContext();
    monitorExit();
    visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable" );
    throwException();
  }

  private static final Type CLASS_TYPE = Type.getType(Class.class);
  private static final org.objectweb.asm.commons.Method FOR_NAME =
    new org.objectweb.asm.commons.Method("forName", "(Ljava/lang/String;)Ljava/lang/Class;");

  public void pushContext() {
    if (method.isStatic()) {
      if ((version & 0xFFFF) < V1_5) {
        push(method.getOwner().replace('/', '.'));
        invokeStatic(CLASS_TYPE, FOR_NAME);
      } else {
        push(method.getOwnerType());
      }
    } else {
      loadThis();
    }
  }
}
