package edu.ucla.pls.wiretap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

// Implementation borrowed from http://korhner.github.io/java/multithreading/detect-java-deadlocks-programmatically/
public class DeadlockDetector implements Runnable {

  private final long interval;
  private final WiretapProperties properties;
  private final Class<?> recorder;

  private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(new ThreadFactory () {
        public Thread newThread(Runnable r) {
          Thread t = new Thread (r);
          t.setDaemon(true);
          return t;
        }
      });

  public DeadlockDetector (Class<?> recorder, WiretapProperties properties,long interval) {
    this.interval = interval;
    this.properties = properties;
    this.recorder = recorder;
  }

  public void start () {
    scheduler.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
  }

  public void run () {
    long [] threadIds = mbean.findMonitorDeadlockedThreads();
    if (threadIds != null) {
      System.err.println("Deadlock detected");
      File deadlockFile = new File(properties.getOutFolder(), "deadlock-threads.txt");
      PrintStream ps;
      try {
        Method m = recorder.getDeclaredMethod("getLogger", Thread.class);
        ps = new PrintStream(deadlockFile);
        ThreadInfo [] threadInfos = mbean.getThreadInfo(threadIds);
        Map<Thread, StackTraceElement[]> stackTraceMap = Thread.getAllStackTraces();
        for (ThreadInfo threadInfo: threadInfos) {
          for (Thread thread : stackTraceMap.keySet()) {
            if (thread.getId() == threadInfo.getThreadId()) {
              ps.println(m.invoke(null, thread));
            }
          }
        }
        ps.println();
        ps.close();
      } catch (FileNotFoundException e) {
        System.err.print("Could not find file: ");
        System.err.println(deadlockFile);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        System.err.println("Could not find potential logger.");
      } catch (InvocationTargetException e) {
        e.printStackTrace();
        System.err.println("Could not find potential logger.");
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
        System.err.println("Could not use the potential logger");
      }
      System.exit(1);
    }
  }

}
