class LockDynamics {

    public void fn (boolean bool) {
        synchronized (this) {
            if (bool) {  
              return;
            } 
        }
    }
    
    public synchronized void fn2 (boolean bool) {
    }
    
    public void fn2_ (boolean bool) {
        try {
            System.out.println("Before lock");
            fn2(true);
        } finally {
            System.out.println("After lock");
        }
    }

    public static void main (String [] args) { 
         new LockDynamics().fn(true);
    }
}
