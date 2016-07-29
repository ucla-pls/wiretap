package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;


/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 */

public class Agent implements ClassFileTransformer {

  private final File folder;
  private final File classfile;

  private BufferedWriter classWriter;

  public Agent (File folder) {
    this.folder = folder;
    this.classfile = new File(folder, "classes.txt");
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
    } catch (IOException e) {
      System.err.println(e);
      System.exit(-1);
    }
  }

  public void close () {
    try {
      classWriter.close();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public void greet() {
    System.err.println("== Running program with Wiretap ==");
    System.err.println("folder    = '" + this.folder + "'");
    System.err.println("classfile = '" + this.classfile + "'");
    System.err.println("==================================");
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
