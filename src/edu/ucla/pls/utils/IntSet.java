package edu.ucla.pls.utils;


/** A fast version of the Set<Integer>, that assumes that the integers
    are small and close together, and that things can't get removed from
    the set.
 */
public class IntSet {

  public final static int INIT_CAP = 1024;

  private final Object lock;

  private boolean [] set;

  public IntSet() {
    this(null, INIT_CAP);
  }

  public IntSet(Object lock, int cap) {
    set = new boolean [cap];
    this.lock = lock == null ? this : lock;
  }

  /** set sets the index to true, and return the previous state of the index.
   */
  public boolean add(int id) {
    boolean [] local = set;
    if (local.length <= id) {
      synchronized (lock) {
        final int size = set.length;
        if (size <= id) {
          local = new boolean [size << 1];
          System.arraycopy(set, 0, local, 0, size);
          set = local;
        }
      }
    }
    if (!local[id]) {
      synchronized (lock) {
        if (!set[id]) {
          set[id] = true;
          return true;
        }
      }
    }
    return false;
  }

  public boolean get(int id) {
    return set[id];
  }
}
