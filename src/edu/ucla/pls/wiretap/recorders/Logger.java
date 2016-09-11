package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.pls.wiretap.WiretapProperties;

/** The logger logs events to file.
 */

public class Logger implements Closeable {

  private static final Map<Thread, Logger> loggers =
    new HashMap<Thread, Logger>();

  private static File logfolder;

  public static void setupRecorder(WiretapProperties properties) {
    logfolder = properties.getLogFolder();
    logfolder.mkdirs();
  }

  public synchronized static void closeRecorder() throws IOException {
    for (Logger logger: loggers.values()) {
      logger.close();
    }
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public synchronized static Logger getLogger(Thread thread) {
    Logger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggers.size();
      File file = new File(logfolder, String.format("%06d.log", id));
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        logger = new Logger(id, writer);
        loggers.put(thread, logger);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return logger;
  }

  public static Logger getRecorder() {
    return getLogger(Thread.currentThread());
  }

  private final int id;
  private final Writer writer;

  public Logger(int id, Writer writer) {
    this.id = id;
    this.writer = writer;
  }

  public void enter(int id) {
    write("E", Integer.toHexString(id));
  }

  public void exit(int id) {
    write("X", Integer.toHexString(id));
  }

  public void fork(Thread thread) {
    write("F", Integer.toHexString(getLogger(thread).getId()));
  }

  public void join(Thread thread) {
    write("J", Integer.toHexString(getLogger(thread).getId()));
  }

  public void read(Object o, int inst) {
    int id = o == null ? 0 : System.identityHashCode(o);
    write("R", Integer.toHexString(inst), Integer.toHexString(id));
  }

  public void request(Object o, int inst) {
    int id = o == null ? 0 : System.identityHashCode(o);
    write("Q", Integer.toHexString(inst), Integer.toHexString(id));
  }

  public void release(Object o, int inst) {
    int id = o == null ? 0 : System.identityHashCode(o);
    write("L", Integer.toHexString(inst), Integer.toHexString(id));
  }

  public void aquire(Object o, int inst) {
    int id = o == null ? 0 : System.identityHashCode(o);
    write("A", Integer.toHexString(inst), Integer.toHexString(id));
  }


  public void write(String event) {
    try {
      writer.write(event);
      writer.write("\n");
    } catch (IOException e) {
      // Silent exception
    }
  }

  public void write(String event, String ... args) {
    try {
      writer.write(event);
      for (String arg: args) {
        writer.write(" ");
        writer.write(arg);
      }
      writer.write("\n");
    } catch (IOException e) {
      // Silent exception
    }
  }

  public int getId() {
    return id;
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

}
