package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 * The agent holds all the information of the run-time of the program.
 */

public class Agent implements ClassFileTransformer, Closeable {

  private final WiretapProperties properties;
  private final MethodHandler methodHandler;
  private final Class<?> recorder;

  private BufferedWriter classWriter;

  private Method closeRecorder;

  public Agent(WiretapProperties properties) {
		this(properties,
         properties.getRecorder(),
         new MethodHandler(properties));
  }

  public Agent (WiretapProperties properties,
                Class<?> recorder,
                MethodHandler methodHandler) {
    this.properties = properties;
    this.methodHandler = methodHandler;
    this.recorder = recorder;
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
      if (properties.getOutFolder().exists()) {
        delete(properties.getOutFolder());
      }
      properties.getOutFolder().mkdirs();

      recorder.getDeclaredMethod("setupRecorder", WiretapProperties.class).invoke(null, properties);
      closeRecorder = recorder.getDeclaredMethod("closeRecorder");
      methodHandler.setup();

      classWriter = new BufferedWriter(new FileWriter(properties.getClassFile()));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    } catch (Exception e) {
      System.err.println("Could not call setup on recorder");
      e.printStackTrace();
      System.exit(-1);
    }

    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            System.err.println("Waiting for the main thread to close... 2s");
            mainThread.join(2000);
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
    methodHandler.close();
    try {
      closeRecorder.invoke(null);
    } catch (Exception e) {
      System.err.println("Could not close recorder");
      e.printStackTrace();
    }
  }

  public MethodHandler getMethodHandler () {
    return this.methodHandler;
  };


  public void greet() {
    System.err.println("====== Running program with Wiretap ======");
    properties.list(System.err);
    System.err.println("==========================================");
  }

  public byte[] transform(ClassLoader loader,
                          String className,
                          Class<?> clazz,
                          ProtectionDomain protectionDomain,
                          byte[] buffer) {

    if (properties.isClassIgnored(className)) {
      return null;
    } else {
      logClass(className, buffer);

      ClassReader reader = new ClassReader(buffer);
      ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
      WiretapClassVisitor wiretap =
        new WiretapClassVisitor(writer,
                                className,
                                properties.getWiretappers(),
                                methodHandler,
                                recorder);

      try {
        reader.accept(wiretap, 0);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }

      byte[] bytes = writer.toByteArray();

      if (properties.doDumpClassFiles()) {
        dumpClassFile(className, bytes);
      }

      return bytes;
    }
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
    File packageFolder = new File(properties.getClassFilesFolder().getValue(), package_);
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

  private static Agent instance;
  /** Create a new agent from the command-line options
   */
  public static Agent fromOptions(String options) {
    instance = new Agent(new WiretapProperties(System.getProperties()));
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
