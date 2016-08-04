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

  @Override
  public void close() throws IOException {
    writer.close();
  }

  public void write(String event) {
    try {
      writer.write(String.format("%s\n", event));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write(String event, String args) {
    try {
      writer.write(String.format("%s %s\n", event, args));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
