package edu.ucla.pls.wiretap;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class handles, and compresses methods.
 */
public class MethodManager implements Closeable {

  private final Map<String, Method> lookup;
  private final List<Method> methods;
  private final WiretapProperties properties;

  private Writer writer;

  public MethodManager(WiretapProperties properties, List<Method> methods) {
    this.methods = methods;
    this.properties = properties;
    this.lookup = new HashMap<String, Method>();

    for (Method method: methods) {
      lookup.put(method.getDescriptor(), method);
    }
  }

  public MethodManager (WiretapProperties properties) {
    this(properties, new ArrayList<Method>());
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
    return lookup.get(descriptor);
  }

  public synchronized Method getMethod(int id) {
    return methods.get(id);
  }

  public synchronized Method createMethod(int access,
                                     String className,
                                     String name,
                                     String typeDesc,
                                     String [] exceptions) {
    Method method = new Method(methods.size(), access,
                        className, name, typeDesc, exceptions);
    methods.add(method);
    try { 
      writer.write(method.getDescriptor());
      writer.write("\n");
    } catch (IOException e) {
      System.err.println("Could not write '" + method.getDescriptor() + "' to file");
    }
    lookup.put(method.getDescriptor(), method);
    return method;
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
        method = createMethod(access, className, name, typeDesc, exceptions);
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
