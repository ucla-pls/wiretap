package edu.ucla.pls.wiretap.managers;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Manager<D,M extends Managable<D>> implements Closeable {

  public final File out;
  public final List<M> managables;

  public final Map<D, M> lookup;

  public Writer writer;

  public Manager(File out, List<M> managables) {
    this.out = out;
    this.managables = managables;
    this.lookup = new HashMap<D, M> ();

    for (M managable: managables) {
      lookup.put(managable.getDescriptor(), managable);
    }
  }

  public Manager(File out) {
    this(out, new ArrayList<M>());
  }

  public synchronized M put(M managable) {
    managable.setId(managables.size());
    D desc = managable.getDescriptor();
    lookup.put(desc, managable);
    managables.add(managable);
    try {
      writer.write(desc.toString());
      writer.write("\n");
    } catch (IOException e) {
      System.err.println("Could not write '" + desc + "' to file");
    }
    return managable;
  }

  public synchronized M get(int id) {
    return managables.get(id);
  }

  public synchronized M get(D descriptor) {
    M managable = getUnsafe(descriptor);
    if (managable != null) {
      return managable;
    } else {
      throw new NoSuchElementException(descriptor.toString());
    }
  }

  public synchronized M getUnsafe(D descriptor) {
    return lookup.get(descriptor);
  }

  public void setup() {
    try {
      writer = new BufferedWriter(new FileWriter(out));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

}
