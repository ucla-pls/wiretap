package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import edu.ucla.pls.utils.IntSet;
import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.InstructionManager;
import edu.ucla.pls.wiretap.MethodManager;
import edu.ucla.pls.wiretap.WiretapProperties;

public class ReachableMethods implements Closeable{

  private static MethodManager handler;
  private static ReachableMethods instance;

  public static void setupRecorder (WiretapProperties properties) {
    handler = Agent.v().getMethodManager();
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
  private final IntSet visitedMethods = new IntSet();

  public ReachableMethods (PrintWriter writer) {
    this.writer = writer;
  }

  public void enter(int id) {
    if (visitedMethods.add(id)) {
      final String desc = handler.getMethod(id).getDescriptor();
      synchronized (this) {
        writer.println(desc);
      }
    }
  }

	@Override
	public void close() throws IOException {
    writer.close();
	}

}
