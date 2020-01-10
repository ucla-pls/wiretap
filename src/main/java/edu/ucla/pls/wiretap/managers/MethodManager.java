package edu.ucla.pls.wiretap.managers;

import java.util.List;

import edu.ucla.pls.wiretap.WiretapProperties;

/** This class handles, and compresses methods.
 */
public class MethodManager extends Manager<String, Method> {

  public MethodManager(WiretapProperties properties, List<Method> methods) {
    super(properties.getMethodFile(), methods);
  }

  public MethodManager (WiretapProperties properties) {
    super(properties.getMethodFile());
  }

  /**
   * getMethodDescriptor returns a unique string describing the method.
   * @return String - a unique string for each method.
   */
  public static String getMethodDescriptor(String owner,
                                           String name,
                                           String desc) {
    StringBuilder b = new StringBuilder();
    b.append(owner).append(".");
    b.append(name);
    b.append(":").append(desc);

    return b.toString();
  }

  public Method getMethod(String owner, String name, String desc) {
    return get(getMethodDescriptor(owner, name, desc));
  }

}
