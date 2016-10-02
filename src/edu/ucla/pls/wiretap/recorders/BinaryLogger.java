package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.pls.wiretap.Formatter;
import edu.ucla.pls.wiretap.WiretapProperties;

/** The logger logs events to file.
 */

public class BinaryLogger implements Closeable {

  private static final Map<Thread, BinaryLogger> loggers =
    new HashMap<Thread, BinaryLogger>();

  private static File logfolder;

  public static void setupRecorder(WiretapProperties properties) {
    logfolder = properties.getLogFolder();
    logfolder.mkdirs();
  }

  public synchronized static void closeRecorder() throws IOException {
    System.out.println("Closing recorders");
    for (BinaryLogger logger: loggers.values()) {
      System.out.println("Closing " + logger);
      logger.close();
    }
  }

  /** getLogger, returns the correct log for this thread. If no log exists
      create a new. getLogger is thread-safe but also slow, so call as little as
      possible. */
  public synchronized static BinaryLogger getBinaryLogger(Thread thread) {
    BinaryLogger logger = loggers.get(thread);
    if (logger == null) {
      int id = loggers.size();
      File file = new File(logfolder, Formatter.format(id, 10, 6) + ".log");
      try {
        OutputStream writer = new BufferedOutputStream(new FileOutputStream(file),
                                                       32768);
        logger = new BinaryLogger(id, writer);
        loggers.put(thread, logger);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return logger;
  }

  public static BinaryLogger getRecorder() {
    return getBinaryLogger(Thread.currentThread());
  }

  public static final int MAX_SIZE = 21;

  private final int id;
  private final OutputStream writer;

  private final byte [] event = new byte[MAX_SIZE];

  public BinaryLogger(int id, OutputStream writer) {
    this.id = id;
    this.writer = writer;
  }

  public String toString() {
    return "Logger_" + this.id;
  }

  private final int ppThread(Thread thread) {
    return getBinaryLogger(thread).getId();
  }

  private final int ppObject(Object object) {
    return object != null ? System.identityHashCode(object) : 0;
  }

  public static final byte FORK = 1;
  public static final byte JOIN = 2;

  public static final byte REQUEST = 3;
  public static final byte ACQUIRE = 4;
  public static final byte RELEASE = 5;

  public static final byte READ = 6;
  public static final byte WRITE = 7;

  public static final int writeInt(int value, byte [] array, int offset) {
    array[offset++] = (byte)(value >>> 24);
    array[offset++] = (byte)(value >>> 16);
    array[offset++] = (byte)(value >>> 8);
    array[offset++] = (byte)(value);
    return offset;
  }

  public final void output(int size) {
    try {
      writer.write(event, 0, size);
    } catch (Exception e) {}
  }

  public final void fork(Thread thread, int inst) {
    int offset = 0;
    event[offset++] = FORK;
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppThread(thread), event, offset);
    output(offset);
  }

  public final void join(Thread thread, int inst) {
    int offset = 0;
    event[offset++] = JOIN;
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppThread(thread), event, offset);
    output(offset);
  }

  public final void request(Object o, int inst) {
    int offset = 0;
    event[offset++] = REQUEST;
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppObject(o), event, offset);
    output(offset);
  }

  public final void release(Object o, int inst) {
    int offset = 0;
    event[offset++] = RELEASE;
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppObject(o), event, offset);
    output(offset);
  }

  public final void acquire (Object o, int inst) {
    int offset = 0;
    event[offset++] = ACQUIRE;
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppObject(o), event, offset);
    output(offset);
  }

  public final void read(Object o, int field, int inst) {
    int offset = 0;
    event[offset++] = (byte) (READ | valueType);
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppObject(o), event, offset);
    offset = writeInt(field, event, offset);
    System.arraycopy(value, 0, event, offset, valueSize);
    output(offset + valueSize);
  }
  public final void readarray(Object a, int index, int inst) {
    read(a, index, inst);
  }

  public final void write(Object o, int field, int inst) {
    int offset = 0;
    event[offset++] = (byte) (WRITE | valueType );
    offset = writeInt(inst, event, offset);
    offset = writeInt(ppObject(o), event, offset);
    offset = writeInt(field, event, offset);
    System.arraycopy(value, 0, event, offset, valueSize);
    output(offset + valueSize);
  }

  public final void writearray(Object a, int index, int inst) {
    write(a, index, inst);
  }

  public static final byte BYTE_TYPE   = 0;
  public static final byte CHAR_TYPE   = (1 << 4);
  public static final byte SHORT_TYPE  = (2 << 4);
  public static final byte INT_TYPE    = (3 << 4);
  public static final byte LONG_TYPE   = (4 << 4);
  public static final byte FLOAT_TYPE  = (5 << 4);
  public static final byte DOUBLE_TYPE = (6 << 4);
  public static final byte OBJECT_TYPE = (7 << 4);

  private byte[] value = new byte [8];
  private int valueSize;
  private byte valueType;

  /** value acts as a store for the next event. It has the ability
      to have a value in memory which can then be used after **/
  public final void value(Object o) {
    valueSize = writeInt(ppObject(o), value, 0);
    valueType = OBJECT_TYPE;
  }

  public final void value(byte v) {
    event[0] = v;
    valueSize = 1;
    valueType = BYTE_TYPE;
  }

  public final void value(char v) {
    event[0] = (byte) v;
    valueSize = 1;
    valueType = CHAR_TYPE;
  }

  public final void value(short v) {
    int offset = 0;
    value[0] = (byte)(v >>> 8);
    value[1] = (byte)(v);
    valueSize = 2;
    valueType = SHORT_TYPE;
  }

  public final void value(int v) {
    valueSize = writeInt(v, value, 0);
    valueType = INT_TYPE;
  }

  public final void value(long v) {
    value[0] = (byte)(v >>> 52);
    value[1] = (byte)(v >>> 48);
    value[2] = (byte)(v >>> 40);
    value[3] = (byte)(v >>> 32);
    value[4] = (byte)(v >>> 24);
    value[5] = (byte)(v >>> 16);
    value[6] = (byte)(v >>> 8);
    value[7] = (byte)(v);
    valueSize = 8;
    valueType = LONG_TYPE;
  }

  public final void value(float v) {
    value(Float.floatToIntBits(v));
    valueType = FLOAT_TYPE;
  }

  public final void value(double v) {
    value(Double.doubleToLongBits(v));
    valueType = DOUBLE_TYPE;
  }

  public int getId() {
    return id;
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

}
