package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;


/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 */

public class Agent implements ClassFileTransformer {

  private final File folder;
  private final File classfile;
  private final File methodfile;

  private BufferedWriter classWriter;
  private BufferedWriter methodWriter;

  public Agent (File folder) {
    this.folder = folder;
    this.classfile = new File(folder, "classes.txt");
    this.methodfile = new File(folder, "methods.txt");
  }

  /** Create a new agent from the command-line options
   */
  public static Agent fromOptions(String options) {
    File folder =
      new File(options != null ? options : "_wiretap").getAbsoluteFile();
    Agent agent =
      new Agent(folder);
    agent.setup();
    return agent;
  }

  public void setup () {

    // Clean up, and make sure that the data is consistent.
    if (folder.exists()) {
      folder.delete();
    }
    folder.mkdirs();

    try {
      classWriter = new BufferedWriter(new FileWriter(this.classfile));
      methodWriter = new BufferedWriter(new FileWriter(this.methodfile));
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-1);
    }
  }

  public void close () {
    try {
      classWriter.close();
      methodWriter.close();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public void greet() {
    System.err.println("====== Running program with Wiretap ======");
    System.err.println("folder     = '" + this.folder + "'");
    System.err.println("classfile  = '" + this.classfile + "'");
    System.err.println("methodfile = '" + this.methodfile + "'");
    System.err.println("==========================================");
  }

  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> clazz,
                          ProtectionDomain protectionDomain,
                          byte[] buffer) {

    System.err.println("Class '" + className + "' has " + buffer.length + " bytes.");

    try {
      classWriter.write(className);
      classWriter.write("\n");
    } catch (IOException e) {
      System.err.println(e);
    }

    ClassReader reader = new ClassReader(buffer);
    reader.accept(new ClassVisitor (Opcodes.ASM5) {
        public MethodVisitor visitMethod (int access,
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
            System.err.println(e);
          }

          return null;
        }
      }, 0);

    // This is the last file loaded by the class-loader, it's a HACK -- fix
    // needed.
    if (className.equals("java/lang/Shutdown$Lock")) {
      close();
    }
    return buffer;
  }

  /** Entry point for the javaagent.
   */
  public static void premain(String options, Instrumentation inst) {
    Agent agent = Agent.fromOptions(options);
    agent.greet();
    inst.addTransformer(agent);
  }

}
