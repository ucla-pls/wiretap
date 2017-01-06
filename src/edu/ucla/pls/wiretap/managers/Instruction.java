package edu.ucla.pls.wiretap.managers;

import edu.ucla.pls.utils.Pair;

public class Instruction extends Managable<Pair<Method, Integer>> {

  private final InstructionDescriptor descriptor;

  public Instruction (Method method, Integer offset) {
    descriptor = new InstructionDescriptor(method, offset);
  }

  public Method getMethod() {
    return descriptor.fst;
  }

  public int getOffset() {
    return descriptor.snd;
  }

  public InstructionDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return descriptor.toString();
  }

  class InstructionDescriptor extends Pair<Method, Integer> {

    public InstructionDescriptor (Method m, Integer offset) {
      super(m, offset);
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
}
