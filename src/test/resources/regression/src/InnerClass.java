/** Example of an inner class
 */
public class InnerClass {

  private int outer;

  public InnerClass () {
    outer = 1;
  }

  public Inner getInner() {
    return new Inner();
  }

  public class Inner {
    private int x;

    public Inner() {
      this.x = outer;
    }

    public int getX () {
      return this.x;
    }

  }

  public static void main(String[] args) {
    setup();
  }

  public static synchronized void setup () {
    InnerClass i = new InnerClass();
    Class<?> clazz = InnerClass.class;
    System.out.println(i.getInner().getX());
  }
}
