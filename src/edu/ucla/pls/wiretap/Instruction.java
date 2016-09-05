package edu.ucla.pls.wiretap;

import edu.ucla.pls.utils.Pair;

public class Instruction extends Pair<Method, Integer> {

  private final int id;

  public Instruction (int id, Method method, Integer offset) {
    super(method, offset);
    this.id = id;
  }

  public Method getMethod() {
    return fst;
  }

  public int getOffset() {
    return snd;
  }

  public int getId () {
    return this.id;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append(fst);
    b.append("!");
    b.append(snd);
    return b.toString();
  }

}
