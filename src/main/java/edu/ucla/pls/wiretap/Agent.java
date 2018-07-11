package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
// import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.ucla.pls.wiretap.ClassSkimmer;
import edu.ucla.pls.wiretap.managers.Field;
import edu.ucla.pls.wiretap.managers.FieldManager;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.managers.Method;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu> The agent holds all the information of the
 *     run-time of the program.
 */
public class Agent implements ClassFileTransformer, Closeable {

  private final WiretapProperties properties;
  private final MethodManager methods;
  private final InstructionManager instructions;
  private final FieldManager fields;
  private final HashMap<String,String> supers;
  private final Class<?> recorder;

  private BufferedWriter classWriter;

  private java.lang.reflect.Method closeRecorder;

  public Agent(WiretapProperties properties) {
    this(
        properties,
        properties.getRecorder(),
        new MethodManager(properties),
        new InstructionManager(properties),
        new FieldManager(properties),
        new HashMap<String, String>());
  }

  public Agent(
      WiretapProperties properties,
      Class<?> recorder,
      MethodManager methods,
      InstructionManager instructions,
      FieldManager fields,
      HashMap<String, String> supers) {
    this.properties = properties;
    this.methods = methods;
    this.recorder = recorder;
    this.instructions = instructions;
    this.fields = fields;
    this.supers = supers;
  }

  private static boolean delete(File f) throws IOException {
    if (f.isDirectory()) {
      for (File c : f.listFiles()) {
        delete(c);
      }
    }
    return f.delete();
  }

  public WiretapProperties getProperties() {
    return this.properties;
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
      methods.setup();
      instructions.setup();
      fields.setup();

      classWriter = new BufferedWriter(new FileWriter(properties.getClassFile()));
    } catch (IOException e) {
      System.err.println("Some IO error occurred");
      e.printStackTrace();
      System.exit(-1);
    } catch (Exception e) {
      System.err.println("Could not call setup on recorder");
      e.printStackTrace();
      System.exit(-1);
    }

    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              public void run() {
                try {
                  System.err.println("Waiting for the main thread to close... 2s");
                  mainThread.join(2000);
                  System.err.println("Closing agent");
                  Agent.v().close();
                  System.err.println("Agent closed... Halting system.");
                  Runtime.getRuntime().halt(1);
                } catch (Exception e) {
                  System.err.println("Could not close agent");
                  e.printStackTrace();
                }
              }
            });
  }

  public void close() throws IOException {
    Thread t =
        new Thread(
            new Runnable() {
              public void run() {
                try {
                  closeRecorder.invoke(null);
                } catch (Exception e) {
                  System.err.println("Could not close recorder:");
                  e.printStackTrace(System.err);
                }
              }
            });
    t.start();
    try {
      t.join(10000);
    } catch (InterruptedException e) {
      System.err.println("Could not close recorder:");
      e.printStackTrace(System.err);
    }
    Closer.close("class writer", classWriter, 1000);
    Closer.close("field writer", fields, 1000);
    Closer.close("method writer", methods, 1000);
    Closer.close("instruction writer", instructions, 1000);
  }

  public MethodManager getMethodManager() {
    return this.methods;
  }

  public InstructionManager getInstructionManager() {
    return this.instructions;
  }

  public FieldManager getFieldManager() {
    return this.fields;
  }

  public void greet() {
    if (properties.isVerbose()) {
      System.err.println("====== Running program with Wiretap ======");
      properties.list(System.err);
      System.err.println("==========================================");
    }
  }

  static double getVersion() {
    String version = System.getProperty("java.version");
    int pos = version.indexOf('.');
    pos = version.indexOf('.', pos + 1);
    return Double.parseDouble(version.substring(0, pos));
  }

  public byte[] transform(
      ClassLoader loader,
      final String className,
      Class<?> clazz,
      ProtectionDomain protectionDomain,
      byte[] buffer) {

    ///System.err.println("+ " + className);
    ClassReader reader = new ClassReader(buffer);


    new ClassSkimmer(className, methods, fields, supers).readFrom(reader);

    for (Field v : new ArrayList<Field>(fields.unverified)) {
      String supero = supers.get(v.getOwner());
      if (supero != null && !supero.equals("java/lang/Object")) {
        synchronized (fields) {
          Field superf = fields.getUnmanaged(new Field(0, supero, v.getName(), v.getDesc(), null));
          fields.unverified.remove(v);
          v.setId(superf.getId());
        }
      }
    }

    if (properties.isClassIgnored(className)) {
      return null;
    } else {
      try {
        classWriter.write(className);
        classWriter.write("\n");
      } catch (IOException e) {
        //Silent exception;
      }

      if (properties.isVerbose()) {
        logClass(className, buffer);
      }

      int flag = ClassWriter.COMPUTE_MAXS;
      if (getVersion() >= 1.7) {
        flag |= ClassWriter.COMPUTE_FRAMES;
      }

      ClassWriter writer = new ClassWriter(reader, flag);
      WiretapClassVisitor wiretap =
          new WiretapClassVisitor(
              writer, className, properties.getRecorder(), properties.getWiretappers(), methods);

      try {
        wiretap.readFrom(reader);
      } catch (Exception e) {
        System.err.println("Could not read from reader");
        e.printStackTrace();
        System.exit(-1);
      }

      byte[] bytes = writer.toByteArray();

      if (properties.doDumpClassFiles()) {
        dumpClassFile(className, bytes);
      }

      if (properties.doVerifyTransformation()) {
        // Test
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
          CheckClassAdapter.verify(new ClassReader(bytes), loader, false, pw);
          String result = sw.toString();
          if (result.length() != 0) {
            System.err.println("Test Failed");
            System.err.println(result);
          }
        } catch (Exception e) {
          System.err.println("Test Failed");
          System.err.println(sw.toString());
          e.printStackTrace();
        }
      }

      return bytes;
    }
  }

  private void logClass(String className, byte[] bytes) {
    System.err.println("Class '" + className + "' has " + bytes.length + " bytes.");
  }

  private void dumpClassFile(String className, byte[] bytes) {
    String package_ = className.split("/[^/]+$")[0];
    String classId;
    File packageFolder;
    if (package_.equals(className)) {
      package_ = "";
      classId = className;
      packageFolder = properties.getClassFilesFolder().getValue();
    } else {
      classId = className.substring(package_.length() + 1);
      packageFolder = new File(properties.getClassFilesFolder().getValue(), package_);
    }
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
  /** Create a new agent from the command-line options */
  public static Agent fromOptions(String options) {
    instance = new Agent(new WiretapProperties(System.getProperties()));
    instance.setup();
    return instance;
  }

  public static Agent v() {
    return instance;
  }

  /** Entry point for the javaagent. */
  public static void premain(String options, Instrumentation inst) {
    Agent agent = Agent.fromOptions(options);
    agent.greet();
    inst.addTransformer(agent);
  }
}
