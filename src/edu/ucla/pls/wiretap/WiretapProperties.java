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

  public File getHistoryFile() {
    final File _default = new File (getOutFolder(), "wiretap.hist");
    return getFile("historyfile", _default);
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

  public File getFieldFile() {
    final File _default = new File (getOutFolder(), "fields.txt");
    return getFile("fieldfile", _default);
  }

  public long getSynchTime() {
    return getLong("synchtime", 1000);
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

  public boolean doVerifyTransformation() {
    return getBoolean("verify", false);
  }

  public boolean isVerbose() {
    return getBoolean("verbose", false);
  }


  public List<Wiretapper> getWiretappers() {
    if (wiretappers == null) {

      List<String> names =
        getList("wiretappers", Arrays.asList(new String [] {
              "EnterMethod",
              "ExitMethod",

              "ReadObject",
              "ReadPrimitive",
              "WriteObject",
              "WritePrimitive",

              "YieldObject",

              "AcquireLock",
              "ReleaseLock",
              "RequestLock",
              "WaitLock",

              "JoinThread",
              "ForkThread"
            }));

      wiretappers = new ArrayList<Wiretapper>();

      Class<?> recorder = getRecorder();

      for (String name: names) {
        String classname = "edu.ucla.pls.wiretap.wiretaps." + name;
        try {
          Class<?> cls = Class.forName(classname);
          Constructor<?> ctor = cls.getConstructor();
          Wiretapper wiretapper = (Wiretapper)ctor.newInstance();
          try {
            wiretapper.setRecorder(recorder);
            wiretappers.add(wiretapper);
          } catch (NoSuchMethodException e) {
            System.err.println("WARNING: " + wiretapper + " not active: " + e.toString());
          }
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
        System.exit(-1);
      } catch (Exception e) {
        System.err.println("Unexpected exception");
        e.printStackTrace();
        System.exit(-1);
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

  public boolean getBoolean(String key, boolean def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    } else {
      return Boolean.parseBoolean(value);
    }
  }

  public long getLong(String key, long def) {
    String value = getProperty(key);
    if (value == null) {
      return def;
    } else {
      return Long.parseLong(value);
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
