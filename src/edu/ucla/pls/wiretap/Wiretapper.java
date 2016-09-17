package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader.OffsetHandler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

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
                         GeneratorAdapter out,
                         Method method,
                         int version) {
    Wiretap tap = createWiretap(next, out);
    tap.setMethod(method);
    tap.setOut(out);
    tap.setVersion(version);
    return tap;
  }


  public abstract Wiretap createWiretap(MethodVisitor next,
                                        GeneratorAdapter out
                                        );


  public abstract class Wiretap extends MethodVisitor implements Opcodes {

    private Method method;
    protected GeneratorAdapter out;
    private int version;

    public Wiretap(MethodVisitor next) {
      super(Opcodes.ASM5, next);
    }

    public void setMethod (Method method) {
      this.method = method;
    }

    public void setVersion (int version) {
      this.version = version;
    }

    public void setOut (GeneratorAdapter out) {
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

    private final Type CLASS_TYPE = Type.getType(Class.class);
    private final org.objectweb.asm.commons.Method FOR_NAME =
      new org.objectweb.asm.commons.Method("forName", "(Ljava/lang/String;)Ljava/lang/Class;");
    public void pushContext() {
      if (getMethod().isStatic()) {
        if (version < V1_5) {
          out.push(method.getOwner().replace('/', '.'));
          out.invokeStatic(CLASS_TYPE, FOR_NAME);
        } else {
          out.push(method.getOwnerType());
        }
      } else {
        out.loadThis();
      }
    }

  }

}
