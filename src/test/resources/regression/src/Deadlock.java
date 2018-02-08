public class Deadlock extends Thread {
  private Deadlock other;
  public void setOther (Deadlock other) {
    this.other = other;
  }

  public synchronized void run () {
    try {
      wait(1000);
      other.lock();
    } catch (InterruptedException e) {
      System.out.println("Unexpected");
    }
  }

  public synchronized void lock() {
    System.out.println("In " + this);
  }

  public static void main(String[] args) {
    Deadlock a = new Deadlock(), b = new Deadlock();
    a.setOther(b);
    b.setOther(a);

    a.start();
    b.start();

    try {
      a.join();
      b.join();
    } catch (InterruptedException e) {
      System.out.println("Unexpected");
    }

    System.out.println("Managed to get through it!");
  }

}
