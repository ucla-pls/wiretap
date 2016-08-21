package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import edu.ucla.pls.wiretap.wiretaps.Basic;

public class Wiretapper extends ClassVisitor {


  private final String className;
  private final MethodHandler methodHandler;

  public Wiretapper(ClassVisitor visitor,
                    String className,
                    MethodHandler methodHandler) {
    super(Opcodes.ASM5, visitor);
    this.className = className;
    this.methodHandler = methodHandler;
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions) {

    Method m = methodHandler.getMethod(access, className, name, desc, exceptions);

    MethodVisitor mv_ =
        super.visitMethod(access, name, desc, signature, exceptions);
    mv_ = new TryCatchBlockSorter(mv_, access, name, desc, signature, exceptions);

    // The use of desc over signature, might be a mistake. Note that signature
    // can be null.
    return new Basic(mv_, m.getDescriptor());
  }
}
