package edu.ucla.pls.wiretap.wiretaps;

import edu.ucla.pls.wiretap.Logger;
import edu.ucla.pls.wiretap.Recorder;

public class BasicRecorder extends Recorder {

  private final Stack frames = new Stack();

  public BasicRecorder(Logger log) {
    super(log);
  }

  /** enterMethod should be called as the first thing when entering
      a method.
  */
  public void enterMethod(String method) {
    frames.push(method);
    log.write("enter", method);
  }

  /** exitMethod should be called as the last thing in a method. The
      method should be the top of the stack. 
  */
  public void exitMethod(String method) {
    String topMethod = this.frames.pop();
    assert topMethod.equals(method);
  }

  /** recoverFrame should be called when the current frame level is known but
      the frame might be wrong.
  */
  public void recoverFrame(String method) {
    this.frames.push(method);
  }

}

class Stack {

  private Node node;

  public String pop() {
    String value = node.value;
    node = node.rest;
    return value;
  }

  public void push(String value) {
    node = new Node(value, node);
  }

  private class Node {
    final String value;
    final Node rest;

    Node(String value, Node rest) {
      this.value = value;
      this.rest = rest;
    }
  }

}
