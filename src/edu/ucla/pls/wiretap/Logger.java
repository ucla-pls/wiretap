package edu.ucla.pls.wiretap;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/** The logger logs events to file.
 */

public class Logger implements Closeable {

  private final int id;
  private final Writer writer;

  public Logger(int id, Writer writer) {
    this.id = id;
    this.writer = writer;
  }

  public static Logger getRecorder() {
    return Agent.v().getLogger(Thread.currentThread());
  }

  public void enter(int id) {
    write("E", Integer.toHexString(id));
  }

  public void write(String event) {
    try {
      writer.write(event);
      writer.write("\n");
    } catch (IOException e) {
      // Silent exception
    }
  }

  public void write(String event, String args) {
    try {
      writer.write(event);
      writer.write(" ");
      writer.write(args);
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
