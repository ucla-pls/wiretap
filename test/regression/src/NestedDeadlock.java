/*
 */

public class NestedDeadlock extends Thread {

  private final Lock a;
  private final Lock b;

  public NestedDeadlock (Lock a, Lock b) {
    this.a = a;
    this.b = b;
  }

  public void run () {
    synchronized (a) {
      synchronized (a) {
          try {
            Thread.sleep(100);
          } catch(Exception e){
            e.printStackTrace();
          } 
        synchronized (b) {
          synchronized (b) {
            System.out.println("Yeah, not in a deadlock");
          }
        }
      }
    }
  }

  public static void main(String [] args) throws Exception {
    Lock a = new Lock(), b = new Lock();

    NestedDeadlock t1 = new NestedDeadlock(a, b);
    NestedDeadlock t2 = new NestedDeadlock(b, a);

    t1.start();
    t2.start();

    t1.join();
    t2.join();
  }

}

class Lock {}
