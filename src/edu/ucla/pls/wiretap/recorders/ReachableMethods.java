package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
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

  private final PrintWriter writer;
  private boolean [] visitedMethods;

  public ReachableMethods (PrintWriter writer) {
    this.writer = writer;
    this.visitedMethods = new boolean [INITIAL_CAP];
  }

  public void enter(int id) {
    boolean [] local = visitedMethods;
    if (local.length <= id) {
      synchronized (this) {
        final int size = visitedMethods.length;
        if (size <= id) {
          local = new boolean [size << 1];
          System.arraycopy(visitedMethods, 0, local, 0, size);
          visitedMethods = local;
        }
      }
    }
    if (!local[id]) {
      synchronized (this) {
        if (!visitedMethods[id]) {
          visitedMethods[id] = true;
          writer.println(handler.getMethod(id).getDescriptor());
        }
      }
    }
	}

	@Override
	public void close() throws IOException {
    writer.close();
	}

}
