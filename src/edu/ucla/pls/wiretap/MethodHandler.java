package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/** This class handles, and compresses methods.
 */
public class MethodHandler implements Closeable {

  private final Map<String, Method> methods;
  private final WiretapProperties properties;

  private Writer writer;

  private int count;

  public MethodHandler(WiretapProperties properties, Map<String, Method> methods) {
    this.methods = methods;
    this.properties = properties;
  }

  public MethodHandler (WiretapProperties properties) {
    this(properties, new HashMap<String, Method>());
  }

  public void setup() {
    final File methodfile = properties.getMethodFile();

    try {
      writer = new BufferedWriter(new FileWriter(methodfile));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

  }

  public void close() throws IOException {
    writer.close();
  }

  public synchronized Method getMethodUnsafe(String descriptor) {
    return methods.get(descriptor);
  }

  public Method getMethod(String descriptor) {
    Method method = getMethodUnsafe(descriptor);
    if (method == null) {
      throw new IllegalArgumentException("Descriptor '" + descriptor +
                                         "' does not point to a method"
                                         );
    } else {
      return method;
    }
  }

  public Method getMethod(int access,
                          String className,
                          String name,
                          String typeDesc,
                          String [] exceptions) {
    final String desc = getMethodDescriptor(className, name, typeDesc);
    Method method;
    synchronized (this) {
      method = getMethodUnsafe(desc);
      if (method == null) {
        method = new Method(count++, access,
                            className, name, typeDesc, exceptions);
        methods.put(desc, method);
      }
    }
    return method;
  }

  /**
   * getMethodDescriptor returns a unique string describing the method.
   * @return String - a unique string for each method.
   */
  public static String getMethodDescriptor(String className,
                                           String methodName,
                                           String typeDesc) {
    StringBuilder b = new StringBuilder();
    b.append(className).append(".");
    if (methodName.charAt(0) == '<') {
			b.append('"').append(methodName).append('"');
		} else {
			b.append(methodName);
		}
		b.append(":").append(typeDesc);

		return b.toString();
	}

}
