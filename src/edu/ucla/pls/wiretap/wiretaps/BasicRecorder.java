package edu.ucla.pls.wiretap.wiretaps;

import edu.ucla.pls.wiretap.Agent;
import edu.ucla.pls.wiretap.Logger;
import edu.ucla.pls.wiretap.Recorder;

public class BasicRecorder extends Recorder {

  private Stack frames;
  // This permssion flag helps removing problems with recursion.
  // The thing is that the BasicRecorder is not instrumented but the
  // Stack frame is.
  private boolean permission = false;

  public BasicRecorder(Logger log) {
    super(log);
  }

  public void setup () {
    frames = new Stack ();
    permission = true;
  }

  /** enterMethod should be called as the first thing when entering
      a method.
  */
  public void enterMethod(String method) {
    if (permission) {
      permission = false;
      frames.push(method);
      log.write("enter", method);
      permission = true;
    }
  }

  /** exitMethod should be called as the last thing in a method. The
      method should be the top of the stack. 
  */
  public void exitMethod(String method) {
    if (permission) {
      permission = false;
      String topMethod = frames.pop();
      assert topMethod.equals(method);
      log.write("exit", method);

      if (frames.isEmpty()) {
        log.write("end");
      }
      permission = true;
    }
  }

  /** forkThread should be called with the forked thread.
   */
  public void forkThread(Thread other) {
    if (permission) {
      permission = false;
      int id = Agent.v().getLogger(other).getId();
      log.write("fork", "t" + id);
      permission = true;
    }
  }

  /** forkThread should be called with the forked thread.
   */
  public void joinThread(Thread other) {
    if (permission) {
      permission = false;
      int id = Agent.v().getLogger(other).getId();
      log.write("join", "t" + id);
      permission = true;
    }
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

  public boolean isEmpty() {
    return node == null;
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
