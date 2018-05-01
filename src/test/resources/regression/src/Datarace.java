public class Datarace extends Thread {
  private static int value;

  public synchronized void run () {
      value++;
  }

  public static void main(String[] args) {
    Datarace a = new Datarace() , b = new Datarace();
    a.start();
    b.start();

    try {
      a.join();
      b.join();
    } catch (InterruptedException e) {
      System.out.println("Unexpected");
    }
    
    System.out.println("Value: " + value);
  }

}
