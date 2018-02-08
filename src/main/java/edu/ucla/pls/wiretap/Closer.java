package edu.ucla.pls.wiretap;

import java.io.Closeable;

public class Closer implements Runnable, Closeable {

  public final String name;
  public final Closeable closeable;
  public final long timelimit;

  public Closer (String name, Closeable closable, long timelimit) {
    this.name = name;
    this.closeable = closable;
    this.timelimit = timelimit;
  }

  public void run () {
    System.err.print("- Closing " + name + "... ");
    System.err.flush();
    try {
      closeable.close();
      System.err.println("Done.");
    } catch (Exception e) {
      System.err.println("Failed.");
      e.printStackTrace(System.err);
    }
  }

  public void close () {
    try {
      Thread t = new Thread(this);
      t.start();
      if (timelimit > 0) {
        t.join(timelimit);
        if (t.isAlive()) {
        }
      } else {
        t.join();
      }
    } catch (InterruptedException e) {
      System.err.println("Couldn't close " + name + " was interrupted.");
      e.printStackTrace(System.err);
    }
  }

  public static void close(String name, Closeable closeable, long timelimit) {
    new Closer(name, closeable, timelimit).close();
  }

}
