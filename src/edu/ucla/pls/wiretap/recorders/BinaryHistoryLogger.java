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
      globalWriter = new BufferedOutputStream(new FileOutputStream(historyFile), 32768);
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

  public final byte[] threadOrderId = new byte[8];
  public int order = 0;

  public BinaryHistoryLogger(int id) {
    super(id, BinaryHistoryLogger.globalWriter);
    writeInt(id, threadOrderId, 0);
  }

  public void output(int size) {
    try {
      writeInt(order++, threadOrderId, 4);
      synchronized (writer) {
        writer.write(event, 0, size);
        writer.write(threadOrderId, 0, size);
      }
    } catch (Exception e) {}
  }

  public final void begin() {
    event[0] = BEGIN;
    output(1);
  }

  public final void end() {
    event[0] = END;
    output(1);
  }

  @Override
  public void close() throws IOException {
    end();
  }

}
