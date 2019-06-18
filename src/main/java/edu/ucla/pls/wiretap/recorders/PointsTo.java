package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.Thread;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Type;

import edu.ucla.pls.wiretap.utils.IntMapSet;
import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Closer;
import edu.ucla.pls.wiretap.DeadlockDetector;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.InstructionManager;

public class PointsTo implements Closeable{

  private static final Map<Thread, PointsTo> recorders =
    new ConcurrentHashMap<Thread, PointsTo>();
  private static final AtomicInteger recorderId = new AtomicInteger();

  private static File folder;

  public static void setupRecorder (WiretapProperties properties) {
    folder = new File(properties.getOutFolder(), "pointsto");
    folder.mkdirs();

    new DeadlockDetector(new DeadlockDetector.Handler () {
        public void handleDeadlock(Thread [] threads) {
          Agent.err.println("Found deadlock, exiting program");
          try {
            PointsTo.closeRecorder();
          } catch (IOException e) {
            e.printStackTrace(Agent.err);
          }
          System.exit(1);
        }
      }, 1000).start();
  }

  public static void closeRecorder() throws IOException {
    Agent.err.println("Closing recorders...");
    for (PointsTo recorder: recorders.values()) {
      Closer.close(recorder.toString(), recorder, 1000);
    }
    Agent.err.println("Done closing recorders...");
  }

  public static PointsTo getRecorder() {
    Thread thread = Thread.currentThread();
    PointsTo instance = recorders.get(thread);
    if (instance == null) {
      try {
        int id = recorderId.getAndIncrement();
        File file = new File(folder, "" + id + ".bin");
        OutputStream writer = new FileOutputStream(file);
        instance = new PointsTo(writer);
        recorders.put(thread, instance);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    }
    return instance;
  }

  private final OutputStream output;
  private final DataOutputStream writer;
  private final ByteArrayOutputStream buffer;
  public PointsTo (OutputStream output) {
    this.output = output;
    this.buffer = new ByteArrayOutputStream(5);
    this.writer = new DataOutputStream(buffer);
  }

  public void beforeCall(int inst) {
    try {
      writer.writeByte(0);
      writer.writeInt(inst);
      buffer.writeTo(output);
      buffer.reset();
    } catch (IOException e) {}
  }

  public void afterCall(int inst) {
    StringBuilder b = new StringBuilder();
    try {
      writer.writeByte(1);
      writer.writeInt(inst);
      buffer.writeTo(output);
      buffer.reset();
    } catch (IOException e) {}
  }

  public void enter(Object o, int method) {
    try {
      writer.writeByte(2);
      writer.writeInt(method);
      buffer.writeTo(output);
      buffer.reset();
    } catch (IOException e) {}
  }

  @Override
  public void close() throws IOException {
    output.flush();
    output.close();
  }

}
