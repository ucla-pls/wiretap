package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
//import org.objectweb.asm.commons.Method;

import edu.ucla.pls.wiretap.managers.Method;

public class RecorderAdapter extends GeneratorAdapter{

  //private final Class<?> recorder;
  private final Type recorderType;
  private final org.objectweb.asm.commons.Method recorderMethod;

  private final Method method;

  private final int version;

  private int local = -1;

  public RecorderAdapter(Class<?> recorder,
                         int version,
                         MethodVisitor mv,
                         Method m
                         ) {
    super(Opcodes.ASM5, mv, m.getAccess(), m.getName(), m.getDesc());
    this.version = version;
    //this.recorder = recorder;
    this.recorderType = Type.getType(recorder);
    this.recorderMethod =
      new org.objectweb.asm.commons.Method("getRecorder", recorderType, new Type [0]);
    this.method = m;
  }


  @Override
  public void visitCode() {
    // In case that it has not been loaded at this point. do it.
    if (local == -1) {
      local = newLocal(recorderType);
      invokeStatic(recorderType, recorderMethod);
      storeLocal(local);
    }
    super.visitCode();
	}

	public void pushRecorder() {
    if (local == -1) {
      local = newLocal(recorderType);
      invokeStatic(recorderType, recorderMethod);
      dup();
      storeLocal(local);
    } else {
      loadLocal(local, recorderType);
    }
  }

  private static final Type CLASS_TYPE = Type.getType(Class.class);
  private static final org.objectweb.asm.commons.Method FOR_NAME =
    new org.objectweb.asm.commons.Method("forName", "(Ljava/lang/String;)Ljava/lang/Class;");

  public void pushContext() {
    if (method.isStatic()) {
      if ((version & 0xFFFF) < Opcodes.V1_5) {
        push(method.getOwner().replace('/', '.'));
        invokeStatic(CLASS_TYPE, FOR_NAME);
      } else {
        push(method.getOwnerType());
      }
    } else {
      loadThis();
    }
  }

}


