package edu.ucla.pls.wiretap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class RecorderAdapter extends GeneratorAdapter{

  private final Class<?> recorder;
  private final Type recorderType;
  private final Method recorderMethod;

  private int local = -1;

  public RecorderAdapter(Class<?> recorder,
                         int api, MethodVisitor mv,
                         int access, String name, String desc) {
    super(api, mv, access, name, desc);
    this.recorder = recorder;
    this.recorderType = Type.getType(recorder);
    this.recorderMethod = new Method("getRecorder", recorderType, new Type [0]);
  }

  public RecorderAdapter(Class<?> recorder,
                         MethodVisitor mv,
                         int access, String name, String desc) {
    this(recorder, Opcodes.ASM5, mv, access, name, desc);
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

}
