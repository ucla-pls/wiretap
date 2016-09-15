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
import edu.ucla.pls.wiretap.managers.FieldManager;
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
  private final FieldManager fields;

  public Logger(int id, Writer writer) {
    this.id = id;
    this.writer = writer;
    this.methods = Agent.v().getMethodManager();
    this.instructions = Agent.v().getInstructionManager();
    this.fields = Agent.v().getFieldManager();
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
    int id;
    if (object != null) {
      id = System.identityHashCode(object);
    } else {
      id = 0;
    }
    return Integer.toHexString(id);
  }

  private String ppField(int id) {
    return fields.get(id).toString();
  }

  private String ppIndex(int index) {
    return Integer.toString(index);
  }

  public void enter(int id) {
    output("enter", ppMethod(id));
  }

  public void exit(int id) {
    output("exit", ppMethod(id));
  }

  public void fork(Thread thread) {
    output("fork", ppThread(thread)) ;
  }

  public void join(Thread thread) {
    output("join", ppThread(thread));
  }

  public void read(Object o, int field, int inst) {
    output("read", ppInst(inst), ppObject(o), ppField(field), value);
  }
  public void readarray(Object a, int index, int inst) {
    output("read", ppInst(inst), ppObject(a), ppIndex(index), value);
  }

  public void write(Object o, int field, int inst) {
    output("write", ppInst(inst), ppObject(o), ppField(field), value);
  }

  public void writearray(Object a, int index, int inst) {
    output("write", ppInst(inst), ppObject(a), ppIndex(index), value);
  }

  public void request(Object o, int inst) {
    output("request", ppInst(inst), ppObject(o));
  }

  public void release(Object o, int inst) {
    output("release", ppInst(inst), ppObject(o));
  }

  public void acquire (Object o, int inst) {
    output("acquire", ppInst(inst), ppObject(o));
  }

  private String value = null;
  /** value acts as a store for the next event. It has the ability
      to have a value in memory which can then be used after **/
  public void value(Object o) {
    value = ppObject(o);
  }

  public void value(char v) {
    value = Character.toString(v);
  }

  public void value(byte v) {
    value = Byte.toString(v);
  }

  public void value(int v) {
    value = Integer.toString(v);
  }

  public void value(short v) {
    value = Short.toString(v);
  }

  public void value(long v) {
    value = Long.toString(v);
  }

  public void value(float v) {
    value = Float.toString(v);
  }

  public void value(double v) {
    value = Double.toString(v);
  }


  public void output(String event) {
    try {
      writer.write(event);
      writer.write("\n");
    } catch (IOException e) {
      // Silent exception
    }
  }

  public void output(String event, String ... args) {
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
