package edu.ucla.pls.wiretap.recorders;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
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


import edu.ucla.pls.wiretap.managers.Method;
import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Closer;
import edu.ucla.pls.wiretap.DeadlockDetector;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.utils.IntSet;
import edu.ucla.pls.wiretap.utils.Maybe;

public class ReachableMethodsAnalyzer implements Closeable{

  private static MethodManager handler;

  private static Set<String> overapproximation;

  private static File unsoundnessfolder;

  private static PrintWriter reachablewriter;
  private static PrintWriter loggedmethods;

  private static ReachableMethodsAnalyzer instance;

  private static HashSet<String> allMethods = new HashSet<String>();

  public static void setupRecorder (WiretapProperties properties) {
    handler = Agent.v().getMethodManager();

    Maybe<Set<String>> methods = properties.getOverapproximation();
    if (methods.hasValue()) {
      System.out.println("Found overapproximation, printing differences");
      overapproximation = methods.getValue();

      unsoundnessfolder = properties.getUnsoundnessFolder();
      unsoundnessfolder.mkdirs();
    }

    File file = new File(properties.getOutFolder(), "reachable.txt");
    File loggedfile = new File(unsoundnessfolder, "methods.txt");

    try {
      reachablewriter = new PrintWriter(new FileWriter(file));
      loggedmethods = new PrintWriter(new FileWriter(loggedfile));
    } catch (IOException e) {
      System.err.println("Could not open file 'reachable.txt' in out folder");
      System.exit(-1);
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

    instance = new ReachableMethodsAnalyzer();
  }

  public static ReachableMethodsAnalyzer getRecorder() {
    return instance;
  }

  public synchronized static void closeRecorder() throws IOException {
    Closer.close("reachablewriter", reachablewriter, 1000);
    Closer.close("loggedmethods", loggedmethods, 1000);
  }

  private final IntSet visitedMethods = new IntSet();

  public static int objectToInt(Object object) {
    return object != null ? System.identityHashCode(object) : 0;
  }

  public void enter(Object obj, int id) {
    if (visitedMethods.add(id)) {
      final String desc = handler.get(id).getDescriptor();
      synchronized (reachablewriter) {
        reachablewriter.println(desc);
        reachablewriter.flush();
      }
      if (overapproximation != null
          && !overapproximation.contains(desc)) {
        printStack(obj, id, desc);
      }
    }
  }

  private synchronized void printStack(Object obj, int id, String desc) {
    PrintWriter stackLogger = null;
    File stackfile = new File(unsoundnessfolder, "" + id + ".stack");

    synchronized (allMethods) {
      loggedmethods.printf("- %s\n", desc);
      loggedmethods.flush();
    }

    try {
      stackLogger = new PrintWriter(stackfile, "UTF-8");

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
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (stackLogger != null) stackLogger.close();
    }

    if (obj != null) {
      File logfile = new File(unsoundnessfolder, "" + id + ".log");
      PrintWriter logger = null;
      try {
        logger = new PrintWriter(logfile, "UTF-8");
        for (String m: getMethods(obj)) {
          Method method = handler.getUnsafe(m);
          String nat = "?";
          if (method != null) {
            nat = method.isNative() ? "t" : "f";
          }
          logger.printf("%s %s\n", m, nat);
        }
        logger.flush();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (logger != null) logger.close();
      }
    }
  }

  private Map<Object, HashSet<String>> methodsPerObject =
    new IdentityHashMap<Object, HashSet<String>>();

  public void returnMethod(Object obj, String name) {
    if (obj != null) {
      synchronized (methodsPerObject) {
        HashSet<String> set = methodsPerObject.get(obj);
        if (set == null) {
          set = new HashSet<String>();
          methodsPerObject.put(obj, set);
        }
        set.add(name);
      }
      synchronized (allMethods) {
        if (allMethods.add(name)) {
          loggedmethods.printf("+ %s\n", name);
          loggedmethods.flush();
        }
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
  public void close() throws IOException {}


  /** https://stackoverflow.com/questions/4024587/get-callers-method-java-lang-reflect-method
   Gets the method from a stack trace. Pretty much stolen from the url above.

  */
  private static String methodFromStackTraceElement(StackTraceElement element) {
    String ownerDot = element.getClassName();
    String name = element.getMethodName();
    int lineNumber = element.getLineNumber();

    String owner = element.getClassName().replace(".", "/");

    if ( lineNumber < 0 ) {
      System.out.println("No Line number");
      return MethodManager.getMethodDescriptor(owner, name, "?");
    }

    String resourceName = "/" + owner + ".class";

    Class<?> clazz = null;
    try {
      clazz = ClassLoader.getSystemClassLoader().loadClass(ownerDot);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return MethodManager.getMethodDescriptor(owner, name, "?");
    }

    InputStream result = clazz.getResourceAsStream(resourceName);

    final AtomicReference<String> reference =
      new AtomicReference<String>();

    if (result == null) {
      System.out.println("Could not read '" + resourceName + "'");
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
