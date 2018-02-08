package edu.ucla.pls.wiretap;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

// Implementation borrowed from http://korhner.github.io/java/multithreading/detect-java-deadlocks-programmatically/
public class DeadlockDetector implements Runnable {

  private final long interval;
  private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
  private final Handler handler;

  public DeadlockDetector (Handler handler, long interval) {
    this.interval = interval;
    this.handler = handler;
  }

  public void start () {
    Thread t = new Thread(this);
    t.setDaemon(true);
    //scheduler.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
    t.start();
    System.err.println("Deadlock detector started!");
  }

  public void run () {
    while (true) {
      try {
        Thread.sleep(interval);
        checkForDeadlock();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void checkForDeadlock () {
    System.err.println("Checking for deadlocks");
    long [] threadIds = mbean.findMonitorDeadlockedThreads();
    if (threadIds != null) {
      System.err.println("Deadlock detected");
      ThreadInfo [] threadInfos = mbean.getThreadInfo(threadIds);
      Map<Thread, StackTraceElement[]> stackTraceMap = Thread.getAllStackTraces();
      Thread [] threads = new Thread [threadInfos.length];
      for (int ii = 0; ii < threadInfos.length; ++ii) {
        long tid = threadInfos[ii].getThreadId();
        for (Thread thread : stackTraceMap.keySet()) {
          if (thread.getId() == tid)
            threads[ii] = thread;
        }
      }
      handler.handleDeadlock(threads);
    }
  }

  public static interface Handler {
    void handleDeadlock(Thread [] threads);
  }

}
