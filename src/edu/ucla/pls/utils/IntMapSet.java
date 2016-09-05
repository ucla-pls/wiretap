package edu.ucla.pls.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;


/** A fast version of the Map<Integer, Set<T>>, that assumes that the integers
    are small and close together, and that things can't get removed from
    the map.
 */
@SuppressWarnings("unchecked")
public class IntMapSet <T> {

  public final static int INIT_CAP = 1024;

  private final Object lock;

  private Set<T> [] map;

  public IntMapSet() {
    this(null, INIT_CAP);
  }

  public IntMapSet(Object lock, int cap) {
    map = (Set<T>[]) new Set<?> [cap];
    this.lock = lock == null ? this : lock;
  }

  public boolean add(int id, T key) {
    Set<T> [] local = map;
    if (local.length <= id) {
      synchronized (lock) {
        final int size = map.length;
        if (size <= id) {
          local = (Set<T>[]) new Set<?> [size << 1];
          System.arraycopy(map, 0, local, 0, size);
          map = local;
        }
      }
    }
    Set<T> set = local[id];
    if (set == null) {
      synchronized (lock) {
        set = map[id];
        if (set == null) {
          set = Collections.<T>newSetFromMap(new ConcurrentHashMap<T, Boolean>());
          map[id] = set;
        }
      }
    }

    return !set.add(key);
  }

  public boolean get(int id, T key) {
    return map[id].contains(key);
  }
}
