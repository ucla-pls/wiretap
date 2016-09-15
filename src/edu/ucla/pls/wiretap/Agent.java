package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.ucla.pls.wiretap.managers.Field;
import edu.ucla.pls.wiretap.managers.FieldManager;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.MethodManager;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 * The agent holds all the information of the run-time of the program.
 */

public class Agent implements ClassFileTransformer, Closeable {

  private final WiretapProperties properties;
  private final MethodManager methods;
  private final InstructionManager instructions;
  private final FieldManager fields;
  private final Class<?> recorder;

  private BufferedWriter classWriter;

  private Method closeRecorder;

  public Agent(WiretapProperties properties) {
		this(properties,
         properties.getRecorder(),
         new MethodManager(properties),
         new InstructionManager(properties),
         new FieldManager(properties));
  }

  public Agent (WiretapProperties properties,
                Class<?> recorder,
                MethodManager methods,
                InstructionManager instructions,
                FieldManager fields) {
    this.properties = properties;
    this.methods = methods;
    this.recorder = recorder;
    this.instructions = instructions;
    this.fields = fields;
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
      methods.setup();
      instructions.setup();
      fields.setup();

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
    methods.close();
    try {
      closeRecorder.invoke(null);
    } catch (Exception e) {
      System.err.println("Could not close recorder");
      e.printStackTrace();
    }
  }

  public MethodManager getMethodManager () {
    return this.methods;
  }

  public InstructionManager getInstructionManager () {
    return this.instructions;
  }

  public FieldManager getFieldManager () {
    return this.fields;
  }

  public void greet() {
    System.err.println("====== Running program with Wiretap ======");
    properties.list(System.err);
    System.err.println("==========================================");
  }

  static double getVersion () {
    String version = System.getProperty("java.version");
    int pos = version.indexOf('.');
    pos = version.indexOf('.', pos+1);
    return Double.parseDouble (version.substring (0, pos));
  }

  public byte[] transform(ClassLoader loader,
                          final String className,
                          Class<?> clazz,
                          ProtectionDomain protectionDomain,
                          byte[] buffer) {

    ClassReader reader = new ClassReader(buffer);
    reader.accept(new ClassVisitor (Opcodes.ASM5)  {

        public FieldVisitor visitField(int access,
                                       String name,
                                       String desc,
                                       String signature,
                                       Object value) {
          // The use of desc over signature, might be a mistake. Note that signature
          // can be null.
          Field f = fields.put(new Field(access, className, name, desc, value));
          return super.visitField(access, name, desc, signature, value);
        }

      }, 0);
    if (properties.isClassIgnored(className)) {
      return null;
    } else {
      logClass(className, buffer);

      int flag = ClassWriter.COMPUTE_MAXS;
      if (getVersion() >= 1.7) {
        flag |= ClassWriter.COMPUTE_FRAMES;
      }

      ClassWriter writer = new ClassWriter(reader, flag);
      WiretapClassVisitor wiretap =
        new WiretapClassVisitor(new CheckClassAdapter(writer),
                                className,
                                properties.getWiretappers(),
                                methods,
                                fields);


      try {
        wiretap.readFrom(reader);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(-1);
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
