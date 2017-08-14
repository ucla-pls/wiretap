package edu.ucla.pls.wiretap.recorders;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;


import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Closer;
import edu.ucla.pls.wiretap.DeadlockDetector;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.utils.IntSet;
import edu.ucla.pls.wiretap.utils.Maybe;

public class ReachableMethodsAnalyzer implements Closeable{

  private static MethodManager handler;

  private static final Map<Thread, ReachableMethodsAnalyzer> loggers =
    new ConcurrentHashMap<Thread, ReachableMethodsAnalyzer>();

  private static File logFolder;
  private static final AtomicInteger loggerId = new AtomicInteger();

  private static Set<String> overapproximation;
  private static Set<String> world;

  private static File unsoundnessfolder;

  public static void setupRecorder (WiretapProperties properties) {
    handler = Agent.v().getMethodManager();

    logFolder = properties.getLogFolder();
    logFolder.mkdirs();

    Maybe<Set<String>> methods = properties.getOverapproximation();
    if (methods.hasValue()) {
      System.out.println("Found overapproximation, printing differences");
      overapproximation = methods.getValue();

      Maybe<Set<String>> worldmethods = properties.getWorld();
      if (worldmethods.hasValue()) {
        String msg = "Found world, excluding differences not present in world";
        System.out.println(msg);
        world = worldmethods.getValue();
      }

      unsoundnessfolder = properties.getUnsoundnessFolder();
      unsoundnessfolder.mkdirs();
    }

    new DeadlockDetector(new DeadlockDetector.Handler () {
        public void handleDeadlock(Thread [] threads) {
          System.out.println("Found deadlock, exiting program");
          try {
            ReachableMethodsAnalyzer.closeRecorder();
          } catch (IOException e) {
            e.printStackTrace();
          }
          System.exit(1);
        }
      }, 1000).start();
  }

  public static ReachableMethodsAnalyzer getLogger(Thread thread) {
    return getReachableMethodsAnalyzer(thread);
  }

  public static ReachableMethodsAnalyzer getRecorder() {
    return getLogger(Thread.currentThread());
  }

  public static ReachableMethodsAnalyzer
    getReachableMethodsAnalyzer(Thread thread)
  {
    ReachableMethodsAnalyzer logger = loggers.get(thread);
    if (logger == null) {
      int id = loggerId.getAndIncrement();
      try {
        File logfile = new File(logFolder, "" + id + ".log");
        OutputStream s = new FileOutputStream(logfile);
        logger = new ReachableMethodsAnalyzer(id, new PrintWriter(s));
        loggers.put(thread, logger);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
    return logger;
  }

  public synchronized static void closeRecorder() throws IOException {
    System.out.println("Closing loggers...");
    for (ReachableMethodsAnalyzer logger: loggers.values()) {
      synchronized (logger) {
        Closer.close(logger.toString(), logger, 1000);
      }
    }
    System.out.println("Done closing loggers...");
  }

  private final PrintWriter writer;
  private final IntSet visitedMethods = new IntSet();

  public ReachableMethodsAnalyzer (int id, PrintWriter writer) {
    this.writer = writer;
  }

  public static int objectToInt(Object object) {
    return object != null ? System.identityHashCode(object) : 0;
  }

  public void enter(Object obj, int id) {
    if (visitedMethods.add(id)) {
      final String desc = handler.get(id).getDescriptor();
      if (overapproximation != null
          && !overapproximation.contains(desc)
          && (world == null || world.contains(desc))) {

        printStack(obj, id, desc);
      }
    }
  }

  private synchronized void printStack(Object obj, int id, String desc) {
    PrintWriter stackLogger = null;
    try {
      stackLogger =
        new PrintWriter(new File(unsoundnessfolder, ""
                                 + id + ".stack.txt"), "UTF-8");

      int i = 0;
      StackTraceElement[] trace =
        Thread.currentThread().getStackTrace();

      stackLogger.printf("%s -1\n", desc);

      stackLogger.flush();

      for (StackTraceElement e : trace) {
        if (++i <= 4) continue;
        String methodName = methodFromStackTraceElement(e);
        stackLogger.printf("%s %s\n", methodName,
                           e.isNativeMethod() ?
                           "Native" :
                           "" + e.getLineNumber());
      }

      stackLogger.flush();

      if (obj != null) {
        stackLogger.println("----");
        stackLogger.flush();
        for (String s: getMethods(obj)) {
          stackLogger.println(s);
        }
      }

      stackLogger.flush();

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (stackLogger != null) stackLogger.close();
    }
  }

  private Map<Object, HashSet<String>> methodsPerObject =
    new HashMap<Object, HashSet<String>>();

  public void returnMethod(Object obj, String m) {
    if (obj != null) {
      synchronized (methodsPerObject) {
        HashSet<String> set = methodsPerObject.get(obj);
        if (set == null) {
          set = new HashSet<String>();
          methodsPerObject.put(obj, set);
        }
        set.add(m);
      }
    }
  }

  public Set<String> getMethods(Object obj) {
    assert obj != null;
    synchronized (methodsPerObject) {
      HashSet<String> s = methodsPerObject.get(obj);
      if (s != null) {
        return (HashSet<String>) s.clone();
      } else {
        return Collections.EMPTY_SET;
      }
    }
  }

  @Override
	public void close() throws IOException {
    writer.close();
  }

  /** https://stackoverflow.com/questions/4024587/get-callers-method-java-lang-reflect-method
   Gets the method from a stack trace. Pretty much stolen from the url above.

  */
  private static String methodFromStackTraceElement(StackTraceElement element) {
    String owner = element.getClassName();
    String name = element.getMethodName();
    int lineNumber = element.getLineNumber();

    if ( lineNumber < 0 ) {
      System.out.println("No Line number");
      return MethodManager.getMethodDescriptor(owner, name, "?");
    }

    String resourceName = "/" + owner.replace(".", "/") + ".class";

    Class<?> clazz = null;
    try {
      clazz = Class.forName(owner);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return MethodManager.getMethodDescriptor(owner, name, "?");
    }

    InputStream result = clazz.getResourceAsStream(resourceName);

    final AtomicReference<String> reference =
      new AtomicReference<String>();

    if (result == null) {
      System.out.println("Cound not read '" + resourceName + "'");
      return MethodManager.getMethodDescriptor(owner, name, "?");
    }

    try {
      ClassReader classReader = new ClassReader(result);
      classReader.accept(new ClassVisitor(Opcodes.ASM5) {

          @Override
          public MethodVisitor visitMethod(int a, final String mname,
                                           final String desc, String signature,
                                           String[] exceptions) {
            if (!name.equals(mname)) return null;

            return new MethodVisitor(Opcodes.ASM5) {
              @Override
              public void visitLineNumber(int line, Label start) {
                if (line == lineNumber) {
                  reference.set(desc);
                }
              }
            };
          }
        }, 0);
    } catch ( IOException e ) {
      // do nothing
      e.printStackTrace();
    } finally {
      try {
      result.close();
      } catch ( IOException e ) {
        // do nothing
        e.printStackTrace();
      }
    }

    String desc = reference.get();
    if (desc == null) {
      System.out.println("Could not find reference");
      return MethodManager.getMethodDescriptor(owner, name, "?");
    } else {
      return MethodManager.getMethodDescriptor(owner, name, desc);
    }
  }


}
