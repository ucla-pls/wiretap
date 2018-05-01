package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ConcurrentHashMap;

import edu.ucla.pls.wiretap.Formatter;
import edu.ucla.pls.wiretap.WiretapProperties;

/** The logger logs events to file.
 */

public class SynchronizedBinaryLogger extends BinaryLogger {

  private static final Map<Thread, SynchronizedBinaryLogger> loggers =
    new ConcurrentHashMap<Thread, SynchronizedBinaryLogger>();

  private static File logfolder;

  // Because of the logic system this variable does not even have to be volatile.
  private static int tick = 0;
  private static final AtomicInteger totalOrderId = new AtomicInteger();
  private static final AtomicInteger loggerId = new AtomicInteger();

  private static Thread synchThread;

  public static void setupRecorder(WiretapProperties properties) {

    logfolder = properties.getLogFolder();
    logfolder.mkdirs();

    System.err.println("BinaryLogger setup with " + logfolder);

    final long synchtime = properties.getSynchTime();
    if (synchtime > 0) {
      synchThread = new Thread(new Runnable () {
          public void run ()  {
            try {
              System.err.println("Synchronizing every " + synchtime + " millis.");
              boolean running = true;
              while (running) {
                Thread.sleep(synchtime);
                tick = tick + 1;
                // if loggers is empty, then the program hasn't runned
                running = loggers.isEmpty();
                for(Thread l: loggers.keySet()) {
                  boolean isAlive = l.isAlive();
                  running |= isAlive;
                }
              }
            }
            catch (InterruptedException e) { return; }
          }
      });
      synchThread.start();
    }
  }

  public static void closeRecorder() throws IOException {
    if (synchThread != null) {
      System.err.println("Interrupting Synch Thread");
      synchThread.interrupt();
    }
    System.err.println("Closing recorders");
    for (BinaryLogger logger: loggers.values()) {
      System.err.println("Closing " + logger);
      logger.close();
    }
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public static SynchronizedBinaryLogger getSynchonizedBinaryLogger(Thread thread) {
    SynchronizedBinaryLogger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggerId.getAndIncrement();
      File file = new File(logfolder, Formatter.format(id, 10, 6) + ".log");
      try {
        OutputStream writer =
          new BufferedOutputStream(new FileOutputStream(file), 32768);
        logger = new SynchronizedBinaryLogger(writer, id);
        loggers.put(thread, logger);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return logger;
  }

  public static SynchronizedBinaryLogger getRecorder() {
    return getSynchonizedBinaryLogger(Thread.currentThread());
  }

  public static final int MAX_SIZE = 21;
  private final OutputStream out;

  private int lastSync = 0;

  public SynchronizedBinaryLogger(OutputStream out, int id) {
    super(new byte[MAX_SIZE], id, null);
    this.out = out;
  }

  @Override
  public BinaryLogger fromThread(Thread thread) {
    return getSynchonizedBinaryLogger(thread);
  }

  @Override
  public void output(byte [] event, int offset, int inst) {
    try {
      out.write(event, 0, offset);
      logInstruction(inst);
    } catch (IOException e) {
    }
  }

  @Override
  public void override() {
    offset = 0;

    int localTick = tick;
    if (localTick != lastSync) {
      int order = totalOrderId.getAndIncrement();
      sync(order);
      lastSync = localTick;
    }
  }

}
