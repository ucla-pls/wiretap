package edu.ucla.pls.wiretap;

public class Formatter {

  public static final char [] ALPHABETH =
    new char [] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static String format(int value, int radix, int size) {
    StringBuilder bldr = new StringBuilder ();
    format(bldr, value, radix, size);
    return bldr.toString();
  }

  public static void format(StringBuilder bldr,
                            int value,
                            final int radix,
                            int size
                            ) {
    while (size-- > 0){
      int multiplier = pow(radix, size);
      int times = value / multiplier;
      bldr.append(ALPHABETH[times]);
      value -= times * multiplier;
    }
  }

  // Taken from http://stackoverflow.com/questions/8071363/calculating-powers-in-java
  private static int pow(int x, int n) {
    if (n == 0) {
      return 1;
    } else if (n == 1) {
      return x;
    } else if ((n & 1) == 0) { //is even
      return pow(x * x, n / 2);
    } else {
      return x * pow(x * x, n / 2);
    }
  }

}
