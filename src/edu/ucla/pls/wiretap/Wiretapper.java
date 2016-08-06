package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.IOException;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import edu.ucla.pls.wiretap.wiretaps.Basic;

public class Wiretapper extends ClassVisitor {


  private final String className;
  private final BufferedWriter methodWriter;

  public Wiretapper(ClassVisitor visitor,
                    String className,
                    BufferedWriter methodWriter) {
    super(Opcodes.ASM5, visitor);
    this.className = className;
    this.methodWriter = methodWriter;
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions) {

    writeMethod(access, name, desc, signature, exceptions);

    MethodVisitor mv_ =
        super.visitMethod(access, name, desc, signature, exceptions);
    mv_ = new TryCatchBlockSorter(mv_, access, name, desc, signature, exceptions);

    // The use of desc over signature, might be a mistake. Note that signature
    // can be null.
    return new Basic(mv_, className + "." + name + ":" + desc);
  }

  public void writeMethod(int access,
                          String name,
                          String desc,
                          String signature,
                          String[] exceptions) {
    try {
      methodWriter.write(className);
      methodWriter.write("/");
      methodWriter.write(name);
      methodWriter.write(desc);
      if (signature != null) {
        methodWriter.write("+");
        methodWriter.write(signature);
      }
      if (exceptions != null) {
        for (String exception: exceptions) {
          methodWriter.write("!");
          methodWriter.write(exception);
        }
      }
      methodWriter.write("\n");
    } catch (IOException e) {
      // Silent
    }
  }
}
