//hello
package edu.ucla.pls.wiretap;

import java.util.HashMap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

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

  @Override
  public Wiretap createWiretap(MethodVisitor next,
                               GeneratorAdapter out
                               ) {
    ValueEmitter emitter = new ValueEmitter(out);
    return createWiretap(next, out, emitter);
  }

  public abstract Wiretap createWiretap(MethodVisitor next,
                                        GeneratorAdapter out,
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

    public final Emitter [] emitters;

    public final HashMap<Integer, HashMap<Integer, Emitter>> byOpcode;

    public ValueEmitter (GeneratorAdapter out) {
      vObject = valueObject.getEmitter(out);
      vChar   = valueChar.getEmitter(out);
      vByte   = valueByte.getEmitter(out);
      vShort  = valueShort.getEmitter(out);
      vLong   = valueLong.getEmitter(out);
      vInt    = valueInt.getEmitter(out);
      vFloat  = valueFloat.getEmitter(out);
      vDouble = valueDouble.getEmitter(out);

      emitters = new Emitter [] {
        vObject, vChar, vByte, vShort, vLong, vInt, vFloat, vDouble
      };

      byOpcode = new HashMap<Integer, HashMap<Integer, Emitter>>();
    }

    public Emitter getTypedEmitter(int opcode, int type) {
      Integer key = Integer.valueOf(type);
      HashMap<Integer, Emitter> map = byOpcode.get(key);
      if (map == null) {
        map = new HashMap<Integer, Emitter>();
        byOpcode.put(key, map);
      }
      key = Integer.valueOf(opcode);
      Emitter emitter = map.get(key);
      if (emitter == null) {
        for (Emitter e: emitters) {
          if (opcode == e.getType(0).getOpcode(type)) {
            emitter = e;
          }
        }
        if (emitter != null) {
          map.put(key, emitter);
        }
      }
      return emitter;
    }

    public Emitter getTypedEmitter(Type type) {
      switch(type.getSort()) {
        case Type.CHAR:    return vChar;
        case Type.BYTE:    return vByte;
        case Type.BOOLEAN: return vByte;
        case Type.SHORT:   return vShort;
        case Type.INT:     return vInt;
        case Type.FLOAT:   return vFloat;
        case Type.LONG:    return vLong;
        case Type.DOUBLE:  return vDouble;
        case Type.ARRAY:   return vObject;
        case Type.OBJECT:  return vObject;
      }
      return null;
    }

    public Emitter getTypedEmitter(String desc) {
      return getTypedEmitter(Type.getType(desc));
    }
  }
}
