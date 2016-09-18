package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import edu.ucla.pls.wiretap.managers.Method;
import edu.ucla.pls.wiretap.managers.MethodManager;

public class WiretapClassVisitor extends ClassVisitor {

  private final String className;
  private final MethodManager methodManager;
  private final List<Wiretapper> wiretappers;
  private final Class<?> recorder;

  private int version;

  public WiretapClassVisitor(ClassVisitor visitor,
                             String className,
                             Class<?> recorder,
                             List<Wiretapper> wiretappers,
                             MethodManager methodManager
                             ) {
    super(Opcodes.ASM5, visitor);
    this.className = className;
    this.methodManager = methodManager;
    this.recorder = recorder;
    this.wiretappers = new ArrayList<Wiretapper>(wiretappers);
    Collections.reverse(this.wiretappers);
  }

  @Override
  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
    this.version = version;
    super.visit(version, access, name, signature, superName, interfaces);
	}

	public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions) {

    // The use of desc over signature, might be a mistake. Note that signature
    // can be null.
    Method m = methodManager.put(new Method(access, className, name, desc, exceptions));

    MethodVisitor visitor =
        super.visitMethod(access, name, desc, signature, exceptions);
    visitor = new TryCatchBlockSorter(visitor, access, name, desc, signature, exceptions);
    RecorderAdapter generator = new RecorderAdapter(recorder, visitor, access, name, desc);

    MethodVisitor next = generator;
    for (Wiretapper wiretapper : wiretappers) {
      next = wiretapper.wiretap(next, generator, m, version);
    }
    return next;
  }

  public void readFrom(ClassReader reader) {
    for (Wiretapper tapper: wiretappers) {
      tapper.setOffsetHandler(reader.getOffsetHandler());
    }
    reader.accept(this, ClassReader.EXPAND_FRAMES);
  }
}
