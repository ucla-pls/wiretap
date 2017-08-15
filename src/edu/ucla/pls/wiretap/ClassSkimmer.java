package edu.ucla.pls.wiretap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.managers.Field;
import edu.ucla.pls.wiretap.managers.FieldManager;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.managers.Method;

public class ClassSkimmer extends ClassVisitor {

  private final MethodManager methods;
  private final FieldManager fields;
  private final String className;

  public ClassSkimmer(String className,
                      MethodManager methods,
                      FieldManager fields) {
    super(Opcodes.ASM5);
    this.methods = methods;
    this.className = className;
    this.fields = fields;
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String [] exceptions) {
    Method m = new Method(access, className, name, desc, exceptions);
    try {
      methods.put(m);
    } catch (Exception e) {
      System.err.println("Warn: trying to add this method again: " + m);
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public FieldVisitor visitField(int access,
                                 String name,
                                 String desc,
                                 String signature,
                                 Object value) {
    // The use of desc over signature, might be a mistake. Note that signature
    // can be null.
    Field f = new Field(access, className, name, desc, value);
    try {
      fields.put(f);
    } catch (Exception e) {
      System.err.println("Warn: trying to add this field again: " + f);
    }
    return super.visitField(access, name, desc, signature, value);
  }

  public void readFrom(ClassReader reader) {
    reader.accept(this, ClassReader.SKIP_CODE);
  }
}
