class WaitProblem {

    public synchronized void waiter () throws InterruptedException { 
      wait(1000);
    }

    public static void main (String [] args) throws Exception {
        new WaitProblem().waiter();
    }
}
