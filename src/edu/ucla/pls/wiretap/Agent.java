package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 * The agent holds all the information of the run-time of the program.
 */

public class Agent implements ClassFileTransformer, Closeable {

  private final File folder;
  private final File classfile;
  private final File classesfolder;
  private final File methodfile;
  private final boolean dumpClassFiles;
  private final List<String> ignore = new ArrayList<String>();

  private final LoggerFactory loggers;

  private BufferedWriter classWriter;
  private BufferedWriter methodWriter;

  public Agent (File folder) {
    this.folder = folder;
    this.classfile = new File(folder, "classes.txt");
    this.methodfile = new File(folder, "methods.txt");
    this.classesfolder = new File (folder, "classes");
    this.loggers = new LoggerFactory(new File(folder, "log"));
    this.dumpClassFiles = true;
  }

  private static boolean delete(File f) throws IOException {
    if (f.isDirectory()) {
      for (File c : f.listFiles()) {
        delete(c);
      }
    }
    return f.delete();
  }

  public void setup() {

    // Clean up, and make sure that the data is consistent.
    ignore.add("edu/ucla/pls/wiretap/wiretaps");

    try {
      if (folder.exists()) {
        delete(folder);
      }
      folder.mkdirs();

      loggers.setup();

      classWriter = new BufferedWriter(new FileWriter(this.classfile));
      methodWriter = new BufferedWriter(new FileWriter(this.methodfile));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            System.err.println("Waiting for the main thread to close...");
            mainThread.join();
            System.err.println("Closing agent");
            Agent.v().close();
            System.err.println("Agent closed");
          } catch (Exception e) {
            System.err.println("Could not close agent");
            e.printStackTrace();
          }
        }
      });
  }

  public void close () throws IOException {
    classWriter.close();
    methodWriter.close();
    loggers.close();
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

    for (String prefix: ignore) {
      if (className.startsWith(prefix)) {
        return null;
      }
    }

    System.err.println("Class '" + className + "' has " + buffer.length + " bytes.");

    try {
      classWriter.write(className);
      classWriter.write("\n");
    } catch (IOException e) {
      //Silent exception;
    }

    ClassReader reader = new ClassReader(buffer);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
    Wiretapper wiretapper = new Wiretapper(writer, className, methodWriter);

    reader.accept(wiretapper, 0);

    byte[] bytes = writer.toByteArray();


    String package_ = className.split("/[^/]+$")[0];
    String classId = className.substring(package_.length() + 1);
    File packageFolder = new File(classesfolder, package_);
    packageFolder.mkdirs();

    File classFile = new File(packageFolder, classId + ".class");

    try {
      FileOutputStream filestream = new FileOutputStream(classFile);
      filestream.write(bytes);
      filestream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return bytes;
  }

  public Logger getLogger(Thread thread) {
    return loggers.getLogger(thread);
  }

  private static Agent instance;
  /** Create a new agent from the command-line options
   */
  public static Agent fromOptions(String options) {
    File folder =
      new File(options != null ? options : "_wiretap").getAbsoluteFile();
    instance = new Agent(folder);
    instance.setup();
    return instance;
  }

  public static Agent v(){
    return instance;
  }

  /** Entry point for the javaagent.
   */
  public static void premain(String options, Instrumentation inst) {
    Agent agent = Agent.fromOptions(options);
    agent.greet();
    inst.addTransformer(agent);
  }

}
