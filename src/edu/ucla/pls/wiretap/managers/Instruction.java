package edu.ucla.pls.wiretap.managers;

import edu.ucla.pls.utils.Pair;

public class Instruction extends Managable<Pair<Method, Integer>> {

  private final Pair<Method, Integer> descriptor;

  public Instruction (Method method, Integer offset) {
    descriptor = new Pair<Method, Integer>(method, offset);
  }

  public Method getMethod() {
    return descriptor.fst;
  }

  public int getOffset() {
    return descriptor.snd;
  }

  public Pair<Method, Integer> getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append(descriptor.fst);
    b.append("!");
    b.append(descriptor.snd);
    return b.toString();
  }

}
