public class Huang14 extends Thread { 
    private static int x = 0;
    private static int y = 0;
    private static int z = 0;

    private static Object l = new Object ();

    public static void main (String[] args) { 
        t1();
    }

    public static void t1 () {
        Thread t2 = new Thread (new T2());
        t2.start();
        synchronized (l) {
          x = 1;
          y = 1;
        }
        try {
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Unexpected");
        }
        if (z == 0) { 
            System.out.println("Error");
        }
    }

    public static void t2 () { 
        int r1 = 0, r2 = 0;
        synchronized (l) {
          r1 = y;
        }
        r2 = x;
        if (r1 == r2) {
            z = 1;
        }
    }

    static class T2 implements Runnable {
        public void run () { 
            t2();
        }
    }

}
