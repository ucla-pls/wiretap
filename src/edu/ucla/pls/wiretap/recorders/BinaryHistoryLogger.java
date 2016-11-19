package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


import edu.ucla.pls.wiretap.WiretapProperties;

/** The logger logs events to file.
 */

public class BinaryHistoryLogger extends BinaryLogger {

  private static final Map<Thread, BinaryHistoryLogger> loggers =
    new ConcurrentHashMap<Thread, BinaryHistoryLogger>();

  private static OutputStream globalWriter;

  private static final AtomicInteger loggerId = new AtomicInteger();

  public static void setupRecorder(WiretapProperties properties) {
    File historyFile = properties.getHistoryFile();
    try {
      globalWriter =
        new BufferedOutputStream(new FileOutputStream(historyFile), 32768);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public synchronized static void closeRecorder() throws IOException {
    System.out.println("Closing recorders");
    for (BinaryHistoryLogger logger: loggers.values()) {
      System.err.println("Closing " + logger);
      logger.close();
    }
    globalWriter.close();
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public static BinaryHistoryLogger getBinaryHistoryLogger(Thread thread) {
    BinaryHistoryLogger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggerId.getAndIncrement();
      logger = new BinaryHistoryLogger(id);
      loggers.put(thread, logger);
    }
    return logger;
  }

  public static BinaryHistoryLogger getRecorder() {
    return getBinaryHistoryLogger(Thread.currentThread());
  }

  public static final int MAX_SIZE = 29;

  public int order = 0;

  public BinaryHistoryLogger(int id) {
    super(BinaryHistoryLogger.globalWriter, new byte[MAX_SIZE], id);
    write(id);
    write(order++);
  }

  @Override
  public void postOutput() {
    offset = 4;
    write(order++);
  }

	@Override
  public BinaryLogger fromThread(Thread thread) {
    return getBinaryHistoryLogger(thread);
  }

  @Override
  public void close() throws IOException {
    end();
  }

}
