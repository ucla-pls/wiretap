//hello
package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;

import edu.ucla.pls.wiretap.EventType.Emitter;

public abstract class ValueWiretapper extends Wiretapper {

  EventType valueObject = declareEventType("value", Object.class);
  EventType valueChar   = declareEventType("value", char.class);
  EventType valueInt    = declareEventType("value", int.class);
  EventType valueByte   = declareEventType("value", byte.class);
  EventType valueShort  = declareEventType("value", short.class);
  EventType valueLong   = declareEventType("value", long.class);
  EventType valueFloat  = declareEventType("value", float.class);
  EventType valueDouble = declareEventType("value", double.class);


  public Wiretap createWiretap(MethodVisitor next,
                               MethodVisitor out
                               ) {
    ValueEmitter emitter = new ValueEmitter(out);
    return createWiretap(next, out, emitter);
  }

  public abstract Wiretap createWiretap(MethodVisitor next,
                                        MethodVisitor out,
                                        ValueEmitter emitter
                                        );

  public class ValueEmitter {

    public final Emitter vObject;

    public final Emitter vChar;

    public final Emitter vByte;
    public final Emitter vShort;
    public final Emitter vLong;
    public final Emitter vInt;

    public final Emitter vFloat;
    public final Emitter vDouble;

    public ValueEmitter (MethodVisitor out) {
      vObject = valueObject.getEmitter(out);
      vChar   = valueChar.getEmitter(out);
      vByte   = valueByte.getEmitter(out);
      vShort  = valueShort.getEmitter(out);
      vLong   = valueLong.getEmitter(out);
      vInt    = valueInt.getEmitter(out);
      vFloat  = valueFloat.getEmitter(out);
      vDouble = valueDouble.getEmitter(out);
    }

  }
}
