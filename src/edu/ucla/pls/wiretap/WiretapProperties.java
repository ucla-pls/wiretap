package edu.ucla.pls.wiretap;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.ucla.pls.utils.Maybe;

public class WiretapProperties extends Properties {
  public static final long serialVersionUID = 1;

  private File outFolder;
  private Collection<String> ignoredPrefixes;

  private List<Wiretapper> wiretappers;
  private Class<?> recorder;

  public WiretapProperties (Properties p) {
    super(p);
  }

  public File getOutFolder() {
    if (outFolder == null) {
      final File _default = new File ("_wiretap");
      outFolder = getFile("outfolder", _default);
    }
    return outFolder;
  }

  public File getLogFolder() {
    final File _default = new File (getOutFolder(), "log");
    return getFile("logfolder", _default);
  }

  public File getClassFile() {
    final File _default = new File (getOutFolder(), "classes.txt");
    return getFile("classfile", _default);
  }

  public File getMethodFile() {
    final File _default = new File (getOutFolder(), "methods.txt");
    return getFile("methodfile", _default);
  }

  public File getInstructionFile() {
    final File _default = new File (getOutFolder(), "instructions.txt");
    return getFile("instructionfile", _default);
  }

  public Collection<String> getIgnoredPrefixes() {
    if (ignoredPrefixes == null) {
      ignoredPrefixes = getList("ignoredprefixes", Arrays.asList("java", "sun", "edu/ucla/pls/wiretap"));
    }
    return ignoredPrefixes;
  }

  public boolean isClassIgnored(String className) {
    for (String prefix: getIgnoredPrefixes()) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  public Maybe<File> getClassFilesFolder() {
    File file = getFile("classfilesfolder", null);
    return Maybe.<File>fromMaybeNull(file);
  }

  public boolean doDumpClassFiles() {
    return getClassFilesFolder().hasValue();
  }

  public List<Wiretapper> getWiretappers() {
    if (wiretappers == null) {
      List<String> names =
        getList("wiretappers", Arrays.asList(new String [] {"EnterMethod"}));
      wiretappers = new ArrayList<Wiretapper>();
      for (String name: names) {
        String classname = "edu.ucla.pls.wiretap.wiretaps." + name;
        try {
          Class<?> cls = Class.forName(classname);
          Constructor<?> ctor = cls.getConstructor();
          wiretappers.add((Wiretapper)ctor.newInstance());
        } catch (ClassNotFoundException e) {
          System.err.println("Could not find class '" + classname + "'");
          e.printStackTrace();
        } catch (NoSuchMethodException e) {
          System.err.println("'" + classname + "' must have an empty constructor");
          e.printStackTrace();
        } catch (Exception e) {
          System.err.println("Unexpected exception");
          e.printStackTrace();
        }
      }
    }
    return wiretappers;
  }

  public Class<?> getRecorder() {
    if (recorder == null) {
      String recorderName =
        "edu.ucla.pls.wiretap.recorders." + getProperty("recorder");
      System.out.println("RecorderName " + recorderName);
      try {
        recorder = Class.forName(recorderName);
      } catch (ClassNotFoundException e) {
        System.err.println("Could not find class '" + recorderName + "'");
        e.printStackTrace();
      } catch (Exception e) {
        System.err.println("Unexpected exception");
        e.printStackTrace();
      }
    }
    return recorder;
  }

  @Override
  public synchronized Object setProperty(String key, String value) {
    throw new UnsupportedOperationException("WiretapProperties are read-only");
  }

  @Override
  public String getProperty(String key, String def) {
    throw new UnsupportedOperationException("Only set defaults through defaults");
  }

  @Override
  public String getProperty(String key) {
    return super.getProperty("wiretap." + key);
  }

  public File getFile(String key, File def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    } else {
      return new File(value);
    }
  }

  public List<String> getList(String key, List<String> def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    } else if ( value.isEmpty()) {
      return Collections.emptyList();
    } else {
      String [] array = value.split(",");
      return Arrays.asList(array);
    }
  }

}
