package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

public class WiretapClassVisitor extends ClassVisitor {

  private final String className;
  private final MethodManager methodManager;
  private final List<Wiretapper> wiretappers;

  public WiretapClassVisitor(ClassVisitor visitor,
                             String className,
                             List<Wiretapper> wiretappers,
                             MethodManager methodManager
                             ) {
    super(Opcodes.ASM5, visitor);
    this.className = className;
    this.methodManager = methodManager;
    this.wiretappers = new ArrayList<Wiretapper>(wiretappers);
    Collections.reverse(this.wiretappers);
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions) {

    Method m = methodManager.getMethod(access, className, name, desc, exceptions);

    MethodVisitor visitor =
        super.visitMethod(access, name, desc, signature, exceptions);
    visitor = new TryCatchBlockSorter(visitor, access, name, desc, signature, exceptions);

    // The use of desc over signature, might be a mistake. Note that signature
    // can be null.
    MethodVisitor next = visitor;
    for (Wiretapper wiretapper : wiretappers) {
      next = wiretapper.wiretap(next, visitor, m);
    }
    return next;
  }

  public void readFrom(ClassReader reader) {
    for (Wiretapper tapper: wiretappers) {
      tapper.setOffsetHandler(reader.getOffsetHandler());
    }
    reader.accept(this, 0);
  }
}
