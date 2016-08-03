package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class LoggerFactory implements Closeable {
  private final Map<Thread, Logger> loggers =
    new HashMap<Thread, Logger>();

  private final File logfolder;

  public LoggerFactory(File logfolder) {
    this.logfolder = logfolder;
  }

  public void setup() {
    logfolder.mkdirs();
  }

  public synchronized void close() throws IOException {
    for (Logger logger: loggers.values()) {
      logger.close();
    }
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public synchronized Logger getLogger(Thread thread) {
    Logger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggers.size();
      File file = new File(logfolder, String.format("%06d.log", id));
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        logger = new Logger(id, writer);
        loggers.put(thread, logger);
      } catch (IOException e) {
        System.err.println(e);
        System.exit(-1);
      }
    }
    return logger;
  }
}
