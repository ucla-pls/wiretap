public class CounterDatarace extends Thread {
  public static Counter
    counter = new Counter(),
    tmp = new Counter();

  public static void main (String [] args) {
    Thread t1 = new CounterDatarace (),
      t2 = new CounterDatarace ();

    try {
      t1.start();
      Thread.sleep(100);
      t2.start();
      t1.join();
      t2.join();
    } catch (InterruptedException e)  {
    }
  }

  public void run() {
    Counter a;
    synchronized (CounterDatarace.class) {
      a = counter;
      counter = tmp;
    }
    a.count++;
    synchronized (CounterDatarace.class) {
      counter = a;
    }
  }

  public static class Counter {
    int count = 0;
  }

}
