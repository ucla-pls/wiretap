package edu.ucla.pls.wiretap.recorders;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.DeadlockDetector;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.managers.MethodManager;
import edu.ucla.pls.wiretap.utils.IntSet;
import edu.ucla.pls.wiretap.utils.Maybe;

public class ReachableMethods implements Closeable{

  private static MethodManager handler;
  private static ReachableMethods instance;

  private static Set<String> overapproximation;
  private static Set<String> world;
  private static File unsoundnessfolder;

  public static void setupRecorder (WiretapProperties properties) {
    handler = Agent.v().getMethodManager();

    Maybe<Set<String>> methods = properties.getOverapproximation();
    if (methods.hasValue()) {
      System.out.println("Found overapproximation, printing differences");
      overapproximation = methods.getValue();

      Maybe<Set<String>> worldmethods = properties.getWorld();
      if (worldmethods.hasValue()) {
        System.out.println("Found world, excluding differences not present in world");
        world = worldmethods.getValue();
      }

      unsoundnessfolder = properties.getUnsoundnessFolder();
      unsoundnessfolder.mkdirs();
    }

    File file = new File(properties.getOutFolder(), "reachable.txt");
    try {
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      instance = new ReachableMethods(writer);
    } catch (IOException e) {
      System.err.println("Could not open file 'reachable.txt' in out folder");
      System.exit(-1);
    }

    new DeadlockDetector(new DeadlockDetector.Handler () {
        public void handleDeadlock(Thread [] threads) {
          System.out.println("Found deadlock, exiting program");
          try {
            ReachableMethods.closeRecorder();
          } catch (IOException e) {
            e.printStackTrace();
          }
          System.exit(1);
        }
      }, 1000).start();

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

  public void enter(Object obj, int id) {
    if (visitedMethods.add(id)) {
      final String desc = handler.get(id).getDescriptor();
      synchronized (this) {
        writer.println(desc);
        writer.flush();
      }
      if (overapproximation != null
          && !overapproximation.contains(desc)
          && (world == null || world.contains(desc))) {

        PrintWriter writer = null;
        try {
          writer = new PrintWriter(new File(unsoundnessfolder, "" + id + ".stack.txt"), "UTF-8");
          StackTraceElement[] trace = Thread.currentThread().getStackTrace();
          int i = 0;
          for (StackTraceElement e : trace) {
            if (++i <= 2) continue;
            writer.println(e.toString());
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (writer != null) writer.close();
        }
      }

    }
  }

	@Override
	public void close() throws IOException {
    writer.close();
	}

}
