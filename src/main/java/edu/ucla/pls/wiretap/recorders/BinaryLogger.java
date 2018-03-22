package edu.ucla.pls.wiretap.recorders;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BinaryLogger implements Closeable {

  public static final int writeInt(int value, byte [] array, int offset) {
    array[offset]     = (byte)(value >>> 24);
    array[offset + 1] = (byte)(value >>> 16);
    array[offset + 2] = (byte)(value >>> 8);
    array[offset + 3] = (byte)(value);
    return offset + 4;
  }

  /** contains the event. */
  protected final byte[] event;

  /** the current offset in the event. */
  protected int offset = 0;

  public final int id;
  public boolean running = false;

  private final OutputStream logInst;

  public BinaryLogger(byte[] event, int id, OutputStream logInst) {
    if (logInst == null)
      throw new NullPointerException("Instruction logger was null");
    this.id = id;
    this.event = event;
    this.logInst = logInst;
  }

  public abstract BinaryLogger fromThread(Thread thread);

  public final int getId() {
    return id;
  }

  public String toString() {
    return "Logger_" + this.id;
  }

  private volatile int lastInstruction;

  protected final void logInstruction(int inst) {
    if (logInst != null) {
      try {
        byte[] bytes = new byte[4];
        writeInt(inst, bytes, 0);
        logInst.write(bytes);
        lastInstruction = inst;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public int getLastInstruction() {
    return lastInstruction;
  }


  public final void write(int value) {
    byte[] _event = this.event;
    int _offset = this.offset;
    _event[_offset]     = (byte)(value >>> 24);
    _event[_offset + 1] = (byte)(value >>> 16);
    _event[_offset + 2] = (byte)(value >>> 8);
    _event[_offset + 3] = (byte)(value);
    this.offset = _offset + 4;
  }

  public static int objectToInt(Object object) {
    return object != null ? System.identityHashCode(object) : 0;
  }

  public final void write(Object object) {
    write(objectToInt(object));
  }

  public abstract void override();
  public abstract void output(byte [] event, int offset, int inst);

  public final void output(int inst) {
    output(event, offset, inst);
    override();
  }

  public static final byte SYNC = 0;
  public static final byte FORK = 1;
  public static final byte JOIN = 2;

  public static final byte REQUEST = 3;
  public static final byte ACQUIRE = 4;
  public static final byte RELEASE = 5;

  public static final byte BEGIN = 6;
  public static final byte END = 7;

  public static final byte BRANCH = 8;
  public static final byte PHI = 9;

  public static final byte ENTER = 10;
  public static final byte EXIT  = 11;

  public static final byte READ = 12;
  public static final byte WRITE = 13;

  public static final byte READARRAY = 14;
  public static final byte WRITEARRAY = 15;

  public final void sync(int order) {
    event[offset] = SYNC;
    write(order);
    output(-1);
  }


  public final void fork(Thread thread, int inst) {
    BinaryLogger logger = fromThread(thread);
    event[offset++] = FORK;
    write(logger.id);
    output(inst);
    logger.begin();
  }

  public final void join(Thread thread, int inst) {
    BinaryLogger logger = fromThread(thread);
    logger.end();
    event[offset++] = JOIN;
    write(logger.id);
    output(inst);
  }

  public final void request(Object o, int inst) {
    event[offset++] = REQUEST;
    write(o);
    output(inst);
  }

  public final void release(Object o, int inst) {
    event[offset++] = RELEASE;
    write(o);
    output(inst);
  }

  public final void acquire (Object o, int inst) {
    event[offset++] = ACQUIRE;
    write(o);
    output(inst);
  }

  private final Lock readlock = new ReentrantLock();
  private final Lock writelock = readlock;

  private final LinkedList<Entry> writestack = new LinkedList<Entry>();

  private class Entry {
    final int inst;
    final int offset;
    final byte [] event;

    Entry(int inst, int offset, byte [] event) {
      this.inst = inst;
      this.offset = offset;
      this.event = event;
    }
  }

  private final void stack(int inst) {
    synchronized (writestack) {
      writestack.push(new Entry(inst, offset, Arrays.copyOf(event, offset)));
      override();
    }
  }

  private final void destack() {
    synchronized (writestack) {
      Entry e = writestack.pop();
      output(e.event, e.offset, e.inst);
    }
  }

  public final void postwrite () {
    destack();
    writelock.unlock();
  }

  public final void preread () {
    readlock.lock();
  }

  public final void read(Object o, int field, int inst) {
    event[offset++] = (byte) (READ | valueType);
    write(o);
    write(field);
    offset += valueSize;
    output(inst);
    readlock.unlock();
  }
  public final void readarray(Object a, int index, int inst) {
    event[offset++] = (byte) (READARRAY | valueType);
    write(a);
    write(index);
    offset += valueSize;
    output(inst);
    readlock.unlock();
  }

  public final void write(Object o, int field, int inst) {
    writelock.lock();
    event[offset++] = (byte) (WRITE | valueType);
    write(o);
    write(field);
    offset += valueSize;
    stack(inst);
  }

  public final void writearray(Object a, int index, int inst) {
    writelock.lock();
    event[offset++] = (byte) (WRITEARRAY | valueType);
    write(a);
    write(index);
    offset += valueSize;
    stack(inst);
  }

  public final void begin() {
    synchronized (this) {
      event[offset++] = BEGIN;
      running = true;
      output(-1);
    }
  }

  public final void end() {
    synchronized (this) {
      if (running) {
        running = false;
        event[offset++] = END;
        output(-1);
      }
    }
  }

  public final void branch(int inst) {
    event[offset++] = BRANCH;
    output(inst);
  }

  public final void enter(Object o, int methodId) {
    event[offset++] = ENTER;
    write(o);
    write(methodId);
    output(-1);
  }

  @Override
	public void close() throws IOException {
    logInst.close();
  }

  public static final int BYTE_TYPE   = 0;
  public static final int CHAR_TYPE   = (1 << 4);
  public static final int SHORT_TYPE  = (2 << 4);
  public static final int INT_TYPE    = (3 << 4);
  public static final int LONG_TYPE   = (4 << 4);
  public static final int FLOAT_TYPE  = (5 << 4);
  public static final int DOUBLE_TYPE = (6 << 4);
  public static final int OBJECT_TYPE = (7 << 4);

  private int valueSize;
  private byte valueType;

  public final void value(byte v) {
    int _offset = this.offset + 9;
    byte[] _event = this.event;
    _event[_offset] = v;
    valueSize = 1;
    valueType = BYTE_TYPE;
  }

  public final void value(char v) {
    value((byte) v);
    valueType = CHAR_TYPE;
  }

  public final void value(short v) {
    int _offset = this.offset + 9;
    byte[] _event = this.event;
    _event[_offset + 0] = (byte)(v >>> 8);
    _event[_offset + 1] = (byte)(v);
    valueSize = 2;
    valueType = SHORT_TYPE;
  }

  public final void value(int v) {
    int _offset = this.offset + 9;
    byte[] _event = this.event;
    _event[_offset + 0] = (byte)(v >>> 24);
    _event[_offset + 1] = (byte)(v >>> 16);
    _event[_offset + 2] = (byte)(v >>> 8);
    _event[_offset + 3] = (byte)(v);
    valueSize = 4;
    valueType = INT_TYPE;
  }

  public final void value(long v) {
    int _offset = this.offset + 9;
    byte[] _event = this.event;
    _event[_offset + 0] = (byte)(v >>> 52);
    _event[_offset + 1] = (byte)(v >>> 48);
    _event[_offset + 2] = (byte)(v >>> 40);
    _event[_offset + 3] = (byte)(v >>> 32);
    _event[_offset + 4] = (byte)(v >>> 24);
    _event[_offset + 5] = (byte)(v >>> 16);
    _event[_offset + 6] = (byte)(v >>> 8);
    _event[_offset + 7] = (byte)(v);
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

  /** value acts as a store for the next event. It has the ability
      to have a value in memory which can then be used after **/
  public final void value(Object o) {
    value(objectToInt(o));
    valueType = OBJECT_TYPE;
  }

}
