// This file closely represent the example in Bensalem and
// Havelunds article "Dynamic Deadlock Analysis of Multi-Threaded
// Programs".

public class Bensalem {

  public static Object
    G = new Object (),
    L1 = new Object(),
    L2 = new Object();

  public static void main (String [] args) {
    new T1().start();
    new T2().start();
  }

  static class T1 extends Thread {
    public void run () {
      synchronized (G) {
        synchronized (L1) {
          synchronized (L2) {}
        }
      }
      T3 t3 = new T3();
      t3.start();
      try {
        t3.join();
      } catch (Exception e) {
      }
      synchronized (L2) {
        synchronized (L1) {
        }
      }
    }
  }

  static class T2 extends Thread {
    public void run () {
      synchronized (G) {
        synchronized (L2) {
          synchronized (L1) {}
        }
      }
    }
  }

  static class T3 extends Thread {
    public void run () {
      synchronized (L1) {
        synchronized (L2) {}
      }
    }
  }

}
