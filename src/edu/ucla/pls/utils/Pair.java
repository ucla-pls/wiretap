package edu.ucla.pls.utils;

import java.util.Objects;

// Partly stolen from http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java

public class Pair<A, B> {

  public final A fst;
  public final B snd;

  public Pair(A fst, B snd) {
    this.fst = fst;
    this.snd = snd;
  }

  public static <A, B> Pair<A, B> of(A fst, B snd) {
    return new Pair<A, B>(fst, snd);
  }

  public boolean equal(Object o) {
    if (o instanceof Pair) {
      final Pair<?, ?> other = (Pair<?,?>) o;
      return o == this ||
        ( Objects.equals(fst, other.fst) && Objects.equals(snd, other.snd) );
    } else {
      return false;
    }
  }

  // From boost::hash_combine
  public int hashCode() {
    int fst_ = fst == null ? 0 : fst.hashCode();
    int snd_ = snd == null ? 0 : snd.hashCode();
    return fst_ ^ (snd_ + 0x9e3779b9 + (fst_ << 6) + (snd_ >> 2));
  }

  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("(").append(fst).append(", ").append(snd).append(")");
    return b.toString();

  }
}
