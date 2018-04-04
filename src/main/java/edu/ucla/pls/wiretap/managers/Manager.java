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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

public class Manager<D,M extends Managable<D>> implements Closeable {

  public final File out;
  public final List<M> managables;
  public final HashSet<M> unverified;

  public final List<M> unmanaged;

  public final Map<D, M> lookup;

  public Writer writer;

  public Manager(File out, List<M> managables) {
    this.out = out;
    this.managables = managables;
    this.lookup = new HashMap<D, M> ();
    this.unverified = new HashSet<M>();
    this.unmanaged = new ArrayList<M>();

    for (M managable: managables) {
      lookup.put(managable.getDescriptor(), managable);
    }
  }

  public Manager(File out) {
    this(out, new ArrayList<M>());
  }

  public synchronized M put(M managable) {
    D desc = managable.getDescriptor();
    M m2 = lookup.get(desc);
    if (m2 != null) {
      throw new RuntimeException("Could not add " + managable + ", it already exist " + m2);
    }
    return putUnsafe(managable);
  }

  public synchronized M putUnsafe(M managable) {
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

  public synchronized void verify(M managable) {
    D desc = managable.getDescriptor();
    M result = getUnsafe(desc);
    putUnsafe(managable);
    if (result != null) {
      // System.err.println("WARN: adding " + managable + " again");
      unverified.remove(result);
      result.setId(managable.getId());
    }
  }

  public synchronized M get(int id) {
    return managables.get(id);
  }

  public synchronized M getUnmanaged(M def) {
    D desc = def.getDescriptor();
    M managable = getUnsafe(desc);
    if (managable == null) {
      managable = def;
      managable.setId(-1 -unmanaged.size());
      unmanaged.add(managable);
      lookup.put(desc, managable);
      unverified.add(managable);
    }
    return managable;
  }

  public synchronized M getDefault(M def) {
    D desc = def.getDescriptor();
    M managable = getUnsafe(desc);
    if (managable == null) {
      managable = put(def);
    }
    return managable;
  }

  public synchronized int check(int id) {
    M managable = unmanaged.get(-1 - id);
    int nid = managable.getId();
    if (nid > 0) {
      return nid;
    } else {
      if (nid != id) {
        managable.setId(check(nid));
      } else {
        System.err.println("WARN: UNVERIFIED " + managable);
        putUnsafe(managable);
      }
      return managable.getId();
    }
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
      System.err.println("Could not open file-writer");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

}
