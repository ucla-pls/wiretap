package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class EventType {

  private final String methodName;
  private final Class<?>[] types;

  private final String signature;

  private String recorder;

  private MethodVisitor out;

  public EventType(String methodName, Class<?> [] types) {
    this.methodName = methodName;
    this.types = types;

    final Type[] types_ = new Type[types.length];
    for (int i = 0, len = types.length ; i != len; i++) {
      types_[i] = Type.getType(types[i]);
    }
    signature = Type.getMethodDescriptor(Type.VOID_TYPE, types_);
  }

  public void setRecorder(Class<?> recorder) throws NoSuchMethodException {
    // Test that method exists.
    recorder.getMethod(methodName, types);
    this.recorder = Type.getInternalName(recorder);
  }

  public Emitter getEmitter(MethodVisitor out) {
    return new Emitter(out);
  }

  public class Emitter {

    private final MethodVisitor out;

    public Emitter(MethodVisitor out) {
      this.out = out;
    }

    public void emit(Object ... args) {

      if (args.length != types.length) {
        throw new IllegalArgumentException("The args needs to be same length " +
                                           " as the designated types");
      }

      // pushRecorder onto the stack
      pushRecorder();

      // Add constants
      for (int i = 0, len = args.length; i != len; ++i) {
        if (primitiveTypeCheck(types[i], args[i])) {
          out.visitLdcInsn(args[i]);
        } else {
          throw new IllegalArgumentException("The arg " + i + " is of the wrong type");
        }
      }

      // Record.
      record();
    }

    public void log(Object... args) {

      if (args.length != types.length - 1) {
        throw new IllegalArgumentException("The args needs to be one shorter" +
                                           " than the designated types");
      }

      if (types[0] == Long.TYPE || types[0] == Double.TYPE) {
        throw new IllegalArgumentException("Can't call log with a double or " +
                                           "a long as the first argument");
      }

      // Dublicate the logged object
      out.visitInsn(Opcodes.DUP);

      consume(args);
    }

    public void consume(Object... args) {

      if (args.length != types.length - 1) {
        throw new IllegalArgumentException("The args needs to be one shorter" +
                                           " than the designated types");
      }

      if (types[0] == Long.TYPE || types[0] == Double.TYPE) {
        throw new IllegalArgumentException("Can't call log with a double or " +
                                           "a long as the first argument");
      }

      // Push the recorder
      pushRecorder();

      // Swap the recorder and the logged object.
      out.visitInsn(Opcodes.SWAP);

      // Add constants
      for (int i = 0, len = args.length; i != len; ++i) {
        if (primitiveTypeCheck(types[i+1], args[i])) {
          out.visitLdcInsn(args[i]);
        } else {
          throw new IllegalArgumentException("The arg " + i + " is of the wrong type");
        }
      }

      // Record.
      record();

    }

    public boolean primitiveTypeCheck(Class<?> c, Object o) {
      if (o instanceof Integer) {
        return c == Integer.TYPE;
      } else if (o instanceof Byte) {
        return c == Byte.TYPE;
      } else if (o instanceof Character) {
        return c == Character.TYPE;
      } else if (o instanceof Float) {
        return c == Float.TYPE;
      } else if (o instanceof Long) {
        return c == Long.TYPE;
      } else if (o instanceof Double) {
        return c == Double.TYPE;
      } else if (o instanceof String) {
        return c == String.class;
      } else if (o instanceof Short) {
        return c == Short.TYPE;
      } else if (o instanceof Boolean) {
        return c == Boolean.TYPE;
      } else {
        throw new IllegalArgumentException("Only Constants allowed");
      }
    }

    /** Pushes the recoder onto the stack */
    public void pushRecorder() {
      out.visitMethodInsn(Opcodes.INVOKESTATIC,
                          recorder, "getRecorder",
                          "()L" + recorder + ";", false);
    }

    public void record() {
      out.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                          recorder, methodName, signature,
                          false);
    }

  }
}