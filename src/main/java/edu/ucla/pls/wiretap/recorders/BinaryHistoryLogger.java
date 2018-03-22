package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.ucla.pls.wiretap.Closer;
import edu.ucla.pls.wiretap.DeadlockDetector;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.utils.ConcurrentOutputStream;

/** The logger logs events to file.
 */

public class BinaryHistoryLogger extends BinaryLogger {

  private static final Map<Thread, BinaryHistoryLogger> loggers =
    new ConcurrentHashMap<Thread, BinaryHistoryLogger>();

  private static OutputStream globalWriter;

  private static File instFolder;
  private static AtomicLong counter;
  private static final AtomicInteger loggerId = new AtomicInteger();

  public static void setupRecorder(final WiretapProperties properties) {
    File historyFile = properties.getHistoryFile();
    new DeadlockDetector(new DeadlockDetector.Handler () {
        public void handleDeadlock(Thread [] threads) {
          File file = new File(properties.getOutFolder(), "deadlocks.txt");
          try {
            PrintStream out = new PrintStream(file);
            for (Thread t: threads) {
              BinaryHistoryLogger hl = getBinaryHistoryLogger(t);
              out.print(hl.getId());
              out.print(" ");
              out.println(hl.getLastInstruction());
            }
            out.close();
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
          System.exit(1);
        }
      }, 1000).start();
    instFolder = properties.getInstFolder();
    long loggingDepth = properties.getLoggingDepth();
    if (loggingDepth > 0) {
      counter = new AtomicLong(loggingDepth);
    }
    try {
      OutputStream s = new FileOutputStream(historyFile);
      globalWriter =
        new ConcurrentOutputStream(new BufferedOutputStream(s, 32768));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static BinaryLogger getLogger(Thread thread) {
    return getBinaryHistoryLogger(thread);
  }

  public synchronized static void closeRecorder() throws IOException {
    System.out.println("Closing loggers...");
    for (BinaryHistoryLogger logger: loggers.values()) {
      Closer.close(logger.toString(), logger, 1000);
    }
    System.out.println("Done closing loggers...");
    Closer.close("the global writer", globalWriter, 100);
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public static BinaryHistoryLogger getBinaryHistoryLogger(Thread thread) {
    BinaryHistoryLogger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggerId.getAndIncrement();
      try {
        File instfile = new File(instFolder, "" + id);
        OutputStream s = new FileOutputStream(instfile);
        s = new ConcurrentOutputStream(new BufferedOutputStream(s, 32768));
        logger = new BinaryHistoryLogger(id, s);
        loggers.put(thread, logger);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
    return logger;
  }

  public static BinaryHistoryLogger getRecorder() {
    return getBinaryHistoryLogger(Thread.currentThread());
  }

  public static final int MAX_SIZE = 29;

  public int order = 0;

  public BinaryHistoryLogger(int id, OutputStream instLogger) {
    super(BinaryHistoryLogger.globalWriter, new byte[MAX_SIZE], id, instLogger);
    offset = 0;
    write(id);
    write(order++);
  }

  @Override
  public void postOutput() {
    offset = 4;
    write(order++);
    if (counter != null && counter.getAndDecrement() <= 0) {
      System.err.println("Reached maximal depth.");
      System.exit(1);
    }
  }

	@Override
  public BinaryLogger fromThread(Thread thread) {
    return getBinaryHistoryLogger(thread);
  }

  @Override
  public void close() throws IOException {
    super.close();
  }

}
