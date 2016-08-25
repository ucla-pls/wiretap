package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class Wiretap extends MethodVisitor {

  protected final MethodVisitor out;
  private final String className;

  public Wiretap(String className, MethodVisitor next, MethodVisitor out) {
    super(Opcodes.ASM5, next);
    this.out = out;
    this.className = className;
  }

  protected void emit(String name, Object ... constants) {
    pushRecorder();
    int size = constants.length;
    String [] types = new String [size];
    for (int i = 0; i != size; i++) {
      final Object constant = constants[i];
      push(constant);
      types[i] = typeof(constant);
    }
    record(name, types);
  }

  protected void pushRecorder() {
    out.visitMethodInsn(Opcodes.INVOKESTATIC,
                        className, "getRecorder",
                        "()L" + className + ";", false);
  }

  protected void record(String name) {
    record(name);
  }

  protected void record(String name, String ... args) {
    String signature = toSignature(args);
    out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        className, name, signature, false);
  }

  protected void push(Object constant) {
    out.visitLdcInsn(constant);
  }

  protected void printerr(String string) {
    out.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System",
                       "err","Ljava/io/PrintStream;");
    out.visitLdcInsn(string);
    out.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                        "println", "(Ljava/lang/String;)V", false);

  }

  protected void printout(String string) {
    out.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System",
                       "out","Ljava/io/PrintStream;");
    out.visitLdcInsn(string);
    out.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
                        "println", "(Ljava/lang/String;)V", false);
  }


  private static String toSignature(String [] args) {
    StringBuilder b = new StringBuilder();
    b.append("(");
    for (String arg: args) {
      b.append(args);
    }
    b.append(")V");
    return b.toString();

  }

  private static String typeof(Object cst) {
    if (cst instanceof Integer) {
      return "I";
    } else if (cst instanceof Byte) {
      return "B";
    } else if (cst instanceof Character) {
      return "C";
    } else if (cst instanceof Float) {
      return "F";
    } else if (cst instanceof Long) {
      return "J";
    } else if (cst instanceof Double) {
      return "D";
    } else if (cst instanceof String) {
      return "Ljava/lang/String;";
    } else if (cst instanceof Short) {
      return "S";
    } else if (cst instanceof Boolean) {
      return "Z";
    } else {
      throw new IllegalArgumentException("Only Constants allowed");
    }
  }

}
