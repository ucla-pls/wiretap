package edu.ucla.pls.wiretap.recorders;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
      Closer.close(logger.toString(), logger, 1000);
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
    final String desc = handler.get(id).getDescriptor();
    writer.printf("E %s %d", desc, objectToInt(obj));
    if (visitedMethods.add(id)) {
      if (overapproximation != null
          && !overapproximation.contains(desc)
          && (world == null || world.contains(desc))) {
        writer.print(" X");

        PrintWriter stackLogger = null;
        try {
          stackLogger = new PrintWriter(new File(unsoundnessfolder, ""
                                            + id + ".stack.txt"), "UTF-8");

          stackLogger.print(objectToInt(obj));
          stackLogger.print(" ");
          stackLogger.println(desc);

          int i = 0;
          StackTraceElement[] trace = Thread.currentThread().getStackTrace();
          for (StackTraceElement e : trace) {
            if (++i <= 2) continue;
            stackLogger.println(e.toString());
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (stackLogger != null) stackLogger.close();
        }
      }
    }
    writer.println();
  }

  public void returnMethod(Object obj, String m) {
    if (obj != null) {
      writer.printf("R %s %d", m, objectToInt(obj));
      writer.println();
    }
  }

  @Override
	public void close() throws IOException {
    writer.close();
  }

}
