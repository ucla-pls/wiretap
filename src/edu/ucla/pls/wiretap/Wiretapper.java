package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader.OffsetHandler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.wiretap.managers.Field;
import edu.ucla.pls.wiretap.managers.FieldManager;
import edu.ucla.pls.wiretap.managers.Instruction;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.Method;
import edu.ucla.pls.wiretap.managers.MethodManager;

public abstract class Wiretapper {

  private OffsetHandler offsetHandler;

  protected final InstructionManager instructions = Agent.v().getInstructionManager();
  protected final MethodManager methods = Agent.v().getMethodManager();
  protected final FieldManager fields = Agent.v().getFieldManager();

  public void setOffsetHandler (OffsetHandler offsetHandler) {
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
                         RecorderAdapter out,
                         Method method
                         ) {
    Wiretap tap = createWiretap(next, out);
    tap.setMethod(method);
    tap.setOut(out);
    return tap;
  }


  public abstract Wiretap createWiretap(MethodVisitor next,
                                        RecorderAdapter out
                                        );


  public abstract class Wiretap extends MethodVisitor implements Opcodes {

    private Method method;
    protected RecorderAdapter out;

    public Wiretap(MethodVisitor next) {
      super(Opcodes.ASM5, next);
    }

    public void setMethod (Method method) {
      this.method = method;
    }

    public void setOut (RecorderAdapter out) {
      this.out = out;
    }

    public Instruction createInstruction() {
      return instructions.put(new Instruction(method, getOffset()));
    }

    public int createInstructionId() {
      return createInstruction().getId();
    }

    public int getFieldId(String owner, String name, String desc) {
      return getField(owner, name, desc).getId();
    }

    public Field getField(String owner, String name, String desc) {
      return fields.getField(owner, name, desc);
    }

    public Method getMethod () {
      return method;
    }

  }

}
