package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.ucla.pls.utils.ConcurrentOutputStream;
import edu.ucla.pls.wiretap.WiretapProperties;

/** The logger logs events to file.
 */

public class BinaryHistoryLogger extends BinaryLogger {

  private static final Map<Thread, BinaryHistoryLogger> loggers =
    new ConcurrentHashMap<Thread, BinaryHistoryLogger>();

  private static OutputStream globalWriter;

  private static File instFolder;
  private static final AtomicInteger loggerId = new AtomicInteger();

  public static void setupRecorder(WiretapProperties properties) {
    File historyFile = properties.getHistoryFile();
    instFolder = properties.getInstFolder();
    try {
      OutputStream s = new FileOutputStream(historyFile);
      globalWriter =
        new ConcurrentOutputStream(new BufferedOutputStream(s, 32768));
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
  }

  private final ReadWriteLock totalorder = new ReentrantReadWriteLock();
  private final Lock readlock = totalorder.readLock();
  private final Lock writelock = totalorder.writeLock();

  public final void prewrite () {
    writelock.lock();
  }

  public final void postwrite () {
    writelock.unlock();
  }

  public final void preread () {
    readlock.lock();
  }

  public final void postread () {
    readlock.unlock();
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
