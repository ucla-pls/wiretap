package edu.ucla.pls.wiretap;


public abstract class Recorder {
  protected final Logger log;

  public Recorder(Logger log) {
    this.log = log;
  }

}
