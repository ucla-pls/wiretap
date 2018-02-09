public class Huang14 { 
    static class T2 extends Thread { public void run () { t2();} }

    private static int x = 0, y = 0, z = 0;

    public static void main (String[] args) throws InterruptedException { 
        t1();
    }

    public static void t1 () throws InterruptedException {
        T2 t2 = new T2();
        t2.start();
        synchronized (Huang14.class) {
          x = 1;
          y = 1;
        }
        t2.join();
        if (z == 0) { 
            System.out.println("Error");
        }
    }

    public static void t2 () { 
        int r1 = 0, r2 = 0;
        synchronized (Huang14.class) {
          r1 = y;
        }
        r2 = x;
        if (r1 == r2) {
            z = 1;
        }
    }


}
