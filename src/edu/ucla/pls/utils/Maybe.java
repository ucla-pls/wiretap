package edu.ucla.pls.utils;

public abstract class Maybe <T> {
  public abstract T getValue();
  public abstract boolean hasValue();

  public static <T> Maybe<T> fromMaybeNull(T object) {
    if (object == null) {
      return new Nothing<T>();
    } else {
      return new Just<T>(object);
    }
  }
}

class Just <T> extends Maybe<T> {
  private final T object;

  public Just (T object) {
    this.object = object;
  }

  public T getValue() {
    return object;
  }

  public boolean hasValue() {
    return true;
  }
}

class Nothing <T> extends Maybe<T> {
  public T getValue() {
    throw new UnsupportedOperationException("Nothing does not have a value");
  }

  public boolean hasValue() {
    return false;
  }
}
