package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.MethodManager;

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
  private final MethodManager methods;
  private final InstructionManager instructions;

  public Logger(int id, Writer writer) {
    this.id = id;
    this.writer = writer;
    this.methods = Agent.v().getMethodManager();
    this.instructions = Agent.v().getInstructionManager();
  }

  private String ppMethod(int id) {
    return methods.get(id).toString();
  }

  private String ppInst(int id) {
    return instructions.get(id).toString();
  }

  private String ppThread(Thread thread) {
    return Integer.toString(getLogger(thread).getId());
  }

  private String ppObject(Object object) {
    int id = object == null ? 0 : System.identityHashCode(object);
    return Integer.toHexString(id);
  }

  private String value = null;
  /** value acts as a store for the next event. It has the ability
      to have a value in memory which can then be used after **/
  public void value(Object o) {
    value = ppObject(o);
  }

  public void enter(int id) {
    write("enter", ppMethod(id));
  }

  public void exit(int id) {
    write("exit", ppMethod(id));
  }

  public void fork(Thread thread) {
    write("fork", ppThread(thread)) ;
  }

  public void join(Thread thread) {
    write("join", ppThread(thread));
  }

  public void read(Object o, int inst) {
    write("read", ppInst(inst), ppObject(o));
  }

  public void write(Object o, int inst) {
    write("write", ppInst(inst), ppObject(o));
  }

  public void request(Object o, int inst) {
    write("request", ppInst(inst), ppObject(o));
  }

  public void release(Object o, int inst) {
    write("release", ppInst(inst), ppObject(o));
  }

  public void acquire (Object o, int inst) {
    write("acquire", ppInst(inst), ppObject(o));
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
