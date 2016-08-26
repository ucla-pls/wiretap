package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Method;
import edu.ucla.pls.wiretap.MethodHandler;
import edu.ucla.pls.wiretap.WiretapProperties;

public class ReachableMethods implements Closeable{

  private static final int INITIAL_CAP = 1024;

  private static MethodHandler handler;
  private static ReachableMethods instance;

  public static void setupRecorder (WiretapProperties properties) {
    handler = Agent.v().getMethodHandler();
    File file = new File(properties.getOutFolder(), "reachable.txt");
    try {
      Writer writer = new BufferedWriter(new FileWriter(file));
      instance = new ReachableMethods(writer);
    } catch (IOException e) {
      System.err.println("Could not open file 'reachable.txt' in out folder");
      System.exit(-1);
    }
  }

  public static void closeRecorder() throws IOException {
    instance.close();
  }

  public static ReachableMethods getRecorder() {
    return instance;
  }

  private final Writer writer;

  private volatile boolean [] visitedMethods;

  public ReachableMethods (Writer writer) {
    this.writer = writer;
    this.visitedMethods = new boolean [INITIAL_CAP];
  }

  public void enter(int id) {
    final boolean [] local = visitedMethods;
    final int size = local.length;
    if (size <= id) {
      synchronized (this) {
        if (visitedMethods.length <= id) {
          boolean [] newarray = new boolean [size << 1];
          for (int ii = size - 1; ii > 0; --ii) {
            newarray[ii] = local[ii];
          }
          visitedMethods = newarray;
        }
      }
    }
    if (!local[id]) {
      synchronized (this) {
        if (!visitedMethods[id]) {
          visitedMethods[id] = true;
          try {
            writer.write(handler.getMethod(id).getDescriptor());
            writer.write("\n");
          } catch (IOException e) {
            System.err.println("Couldn't access reachable file");
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
