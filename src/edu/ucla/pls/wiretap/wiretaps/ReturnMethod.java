package edu.ucla.pls.wiretap.wiretaps;

import java.io.InputStream;
import java.io.IOException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.ClassSkimmer;
import edu.ucla.pls.wiretap.EventType;
import edu.ucla.pls.wiretap.EventType.Emitter;
import edu.ucla.pls.wiretap.RecorderAdapter;
import edu.ucla.pls.wiretap.Wiretapper;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.managers.Method;

public class ReturnMethod extends Wiretapper {

  EventType returnMethod =
    declareEventType("returnMethod", Object.class, int.class);

  static final String[] LIBRARY_PREFIXES = new String [] {
    "java.",
    "sun.",
    "javax.",
    "com.sun.",
    "com.ibm.",
    "org.xml.",
    "org.w3c.",
    "apple.awt.",
    "com.apple."
  };

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               final RecorderAdapter out) {
    final Emitter returnMethod = this.returnMethod.getEmitter(out);
    final MethodManager methods = this.methods;
    return new Wiretap(next) {

      @Override
      public void visitMethodInsn(int opcode, String owner, String name,
                                  String desc, boolean itf) {

        super.visitMethodInsn(opcode, owner, name, desc, itf);

        // If the return type is not an object skip this
        if (Type.getReturnType(desc).getSort() != Type.OBJECT) {
          return;
        }

        String m = MethodManager.getMethodDescriptor(owner, name, desc);
        Method method = methods.getUnsafe(m);

        // I method does not exist load the class
        if (method == null) {
          loadClass(owner);
          method = methods.getUnsafe(m);

          // If it did not work skip the method
          if (method == null) {
            System.err.println("Could not find " + m);
            return;
          }
        }

        // Log native methods
        if (method.isNative()) {
          returnMethod.log(method.getId());
          return;
        }

        // Log library methods
        for (int i = LIBRARY_PREFIXES.length - 1; i != 0; --i) {
          if (owner.startsWith(LIBRARY_PREFIXES[i])) {
            returnMethod.log(method.getId());
            break;
          }
        }
      }
    };
  }

  private void loadClass(String owner) {
    if (owner.startsWith("[")) return;
    Class<?> clazz = null;
    try {
      clazz = Class.forName(owner.replace("/", "."));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return;
    }
    String resourceName = "/" + owner + ".class";
    InputStream result = clazz.getResourceAsStream(resourceName);

    if (result == null) {
      System.err.println("Could not read '" + resourceName + "'");
      return;
    }

    try {
      new ClassSkimmer(owner, methods, fields)
        .readFrom(new ClassReader(result));
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        result.close();
      } catch ( IOException e ) {
        // do nothing
        e.printStackTrace();
      }
    }

  }
}
