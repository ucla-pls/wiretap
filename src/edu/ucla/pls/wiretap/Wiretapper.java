package edu.ucla.pls.wiretap;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader$OffsetHandler;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ucla.pls.utils.Pair;
import edu.ucla.pls.wiretap.managers.FieldManager;
import edu.ucla.pls.wiretap.managers.Instruction;
import edu.ucla.pls.wiretap.managers.InstructionManager;
import edu.ucla.pls.wiretap.managers.Method;
import edu.ucla.pls.wiretap.managers.MethodManager;

public abstract class Wiretapper {

  private ClassReader$OffsetHandler offsetHandler;

  protected final InstructionManager instructions = Agent.v().getInstructionManager();
  protected final MethodManager methods = Agent.v().getMethodManager();
  protected final FieldManager fields = Agent.v().getFieldManager();

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
    Wiretap tap = createWiretap(next, out);
    tap.setMethod(method);
    tap.setOut(out);
    return tap;
  }


  public abstract Wiretap createWiretap(MethodVisitor next,
                                        MethodVisitor out
                                        );


  public abstract class Wiretap extends MethodVisitor implements Opcodes {

    private Method method;
    protected MethodVisitor out;

    public Wiretap(MethodVisitor next) {
      super(Opcodes.ASM5, next);
    }

    public void setMethod (Method method) {
      this.method = method;
    }

    public void setOut (MethodVisitor out) {
      this.out = out;
    }

    public Instruction createInstruction() {
      return instructions.put(new Instruction(method, getOffset()));
    }

    public int createInstructionId() {
      return createInstruction().getId();
    }

    public int getFieldId(String owner, String name, String desc) {
      return fields.getField(owner, name, desc).getId();
    }

    public Method getMethod () {
      return method;
    }

    public void pushThis() {
      // Load this
      out.visitVarInsn(ALOAD, 0);
    }

    public void pushClass() {
      out.visitFieldInsn(GETSTATIC,
                         getMethod().getOwner(),
                         "class",
                         "Ljava/lang/Class;");
    }

    public void pushContext() {
      if (getMethod().isStatic()) {
        pushClass();
      } else {
        pushThis();
      }
    }

  }

  public abstract class LocalWiretap extends Wiretap {
    private int max;
    // We might need a bigger array;
    private int[] movedTo = new int[100];

    public LocalWiretap (MethodVisitor next) {
      super(next);
    }

    public int getFreeLocal() {
      int freeVar = ++max;
      movedTo[freeVar] = -1;
      return freeVar;
    }

    @Override
    public void visitCode() {
      max = getMethod().getNumberOfArgumentLocals();
      super.visitCode();
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      if (var > max) {
        max = var;
      }

      if (movedTo[var] == -1) {
        movedTo[var] = getFreeLocal();
      } else if (movedTo[var] == 0) {
        movedTo[var] = var;
      }

      super.visitVarInsn(opcode, movedTo[var]);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      super.visitMaxs(maxStack, max);
    }

  }

}
