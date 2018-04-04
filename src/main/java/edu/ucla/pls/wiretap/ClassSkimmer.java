package edu.ucla.pls.wiretap;

import java.util.HashMap;

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
  private final HashMap<String, String> supers;

  public ClassSkimmer(String className,
                      MethodManager methods,
                      FieldManager fields,
                      HashMap<String, String> supers) {
    super(Opcodes.ASM5);
    this.methods = methods;
    this.className = className;
    this.fields = fields;
    this.supers = supers;
  }

  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
    supers.put(className, superName);
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String [] exceptions) {
    Method m = new Method(access, className, name, desc, exceptions);
    methods.verify(m);
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
    fields.verify(f);
    return super.visitField(access, name, desc, signature, value);
  }

  public void readFrom(ClassReader reader) {
    reader.accept(this, ClassReader.SKIP_CODE);
  }
}
