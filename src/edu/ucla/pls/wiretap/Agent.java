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
import java.util.Arrays;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 * The agent holds all the information of the run-time of the program.
 */

public class Agent implements ClassFileTransformer, Closeable {

  private final Properties properties;
  private final LoggerFactory loggers;

  private BufferedWriter classWriter;
  private BufferedWriter methodWriter;

  public Agent(Properties properties) {
    this.properties = properties;
    this.loggers = new LoggerFactory(properties.logfolder);
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
    try {
      if (properties.folder.exists()) {
        delete(properties.folder);
      }
      properties.folder.mkdirs();

      loggers.setup();

      classWriter = new BufferedWriter(new FileWriter(properties.classfile));
      methodWriter = new BufferedWriter(new FileWriter(properties.methodfile));
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
    properties.print(System.err);
    System.err.println("==========================================");
  }

  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> clazz,
                          ProtectionDomain protectionDomain,
                          byte[] buffer) {

    if (properties.isIgnored(className)) {
      return null;
    }

    logClass(className, buffer);

    ClassReader reader = new ClassReader(buffer);
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
    Wiretapper wiretapper = new Wiretapper(writer, className, methodWriter);

    reader.accept(wiretapper, 0);

    byte[] bytes = writer.toByteArray();

    if (properties.doDumpClassFiles()) {
      dumpClassFile(className, bytes);
    }

    return bytes;
  }

  private void logClass(String className, byte[] bytes)  {
    System.err.println("Class '" + className + "' has " + bytes.length + " bytes.");

    try {
      classWriter.write(className);
      classWriter.write("\n");
    } catch (IOException e) {
      //Silent exception;
    }
  }

  private void dumpClassFile(String className, byte[] bytes) {
    String package_ = className.split("/[^/]+$")[0];
    String classId = className.substring(package_.length() + 1);
    File packageFolder = new File(properties.classesfolder, package_);
    packageFolder.mkdirs();

    File classFile = new File(packageFolder, classId + ".class");

    try {
      FileOutputStream filestream = new FileOutputStream(classFile);
      filestream.write(bytes);
      filestream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    String [] ignorePrefixes = new String[] { "edu/ucla/pls/wiretap"};
    instance = new Agent(new Properties(folder, Arrays.asList(ignorePrefixes)));
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
