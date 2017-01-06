package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.objectweb.asm.Type;

import edu.ucla.pls.utils.IntMapSet;
import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.InstructionManager;

public class PointsTo implements Closeable{

  private static PointsTo instance;

  public static void setupRecorder (WiretapProperties properties) {
    File file = new File(properties.getOutFolder(), "pointsto.txt");
    try {
      Writer writer = new BufferedWriter(new FileWriter(file));
      instance = new PointsTo(writer, Agent.v().getInstructionManager());
    } catch (IOException e) {
      System.err.println("Could not open file 'reachable.txt' in out folder");
      System.exit(-1);
    }
  }

  public static void closeRecorder() throws IOException {
    instance.close();
  }

  public static PointsTo getRecorder() {
    return instance;
  }

  private final Writer writer;

  private final InstructionManager instructions;
  private final IntMapSet<Class<?>> pointsto;

  public PointsTo (Writer writer, InstructionManager instructions) {
    this.writer = writer;
    this.pointsto = new IntMapSet<Class<?>>();
    this.instructions = instructions;
  }

  public void read(Object o, int inst) {
    writeIfFirst(inst, o);
  }

  public void yield(Object o, int inst) {
    writeIfFirst(inst, o);
  }

  private final void writeIfFirst(int inst, Object o) {
    if (o != null) {
      final Class<?> c = o.getClass();
      if (pointsto.add(inst, c)) {
        final String clname = Type.getInternalName(c);
        final String instname  = instructions.get(inst).toString();
        synchronized(this) {
          try {
            writer.write(instname);
            writer.write("@");
            writer.write(clname);
            writer.write("\n");
          } catch (IOException e) {
            // Swallow exceptions;
          }
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

}
