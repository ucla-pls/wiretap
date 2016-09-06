package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader$OffsetHandler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public abstract class Wiretapper {

  private ClassReader$OffsetHandler offsetHandler;

  protected final InstructionManager instructions = Agent.v().getInstructionManager();
  protected final MethodManager methods = Agent.v().getMethodManager();

  public void setOffsetHandler (ClassReader$OffsetHandler offsetHandler) {
    this.offsetHandler = offsetHandler;
  }

  public int getOffset() {
    return offsetHandler.getOffset();
  }

  public List<EventType> eventTypes = new ArrayList<EventType>();

  public EventType declareEventType(String name, Class<?> ... types) {
    EventType eventType = new EventType(name, types);
    eventTypes.add(eventType);
    return eventType;
  }

  public void setRecorder(Class<?> recorder) throws NoSuchMethodException {
    for (EventType eventType: eventTypes) {
      eventType.setRecorder(recorder);
    }
  }

  public Wiretap wiretap(MethodVisitor next,
                         MethodVisitor out,
                         Method method) {
    Wiretap tap = createWiretap(next, out, method);
    return tap;
  }


  public abstract Wiretap createWiretap(MethodVisitor next,
                                        MethodVisitor out,
                                        Method method);


  public abstract class Wiretap extends MethodVisitor {

    public Wiretap(MethodVisitor next) {
      super(Opcodes.ASM5, next);
    }

  }
}
