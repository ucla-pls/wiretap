package edu.ucla.pls.wiretap.managers;

public abstract class Managable<D> {
  private int id;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public abstract D getDescriptor();

  @Override
  public String toString() {
    return getDescriptor().toString();
  }

  @Override
  public int hashCode() {
    return getDescriptor().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Managable<?>) {
      Object d = ((Managable<?>)obj).getDescriptor();
      return d != null && d.equals(getDescriptor());
    } else {
      return false;
    }
  }
}
