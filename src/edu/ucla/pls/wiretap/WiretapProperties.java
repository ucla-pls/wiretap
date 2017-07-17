package edu.ucla.pls.wiretap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.ucla.pls.wiretap.utils.Maybe;

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

  public File getInstFolder() {
    final File _default = new File (getOutFolder(), "instructions");
    final File folder = getFile("instfolder", _default);
    if (! folder.exists()) {
      folder.mkdirs();
    }
    return folder;
  }

  public File getClassFile() {
    final File _default = new File (getOutFolder(), "classes.txt");
    return getFile("classfile", _default);
  }

  public File getMethodFile() {
    final File _default = new File (getOutFolder(), "methods.txt");
    return getFile("methodfile", _default);
  }

  public File getUnsoundnessFile() {
    final File _default = new File (getOutFolder(), "unsoundness.txt");
    return getFile("unsoundnessfile", _default);
  }

  public Maybe<File> getOverapproximationFile() {
    File file = getFile("overapproximation", null);
    return Maybe.<File>fromMaybeNull(file);
  }

  public Maybe<Set<String>> getOverapproximation() {
    Maybe<File> file = getOverapproximationFile();
    if (file.hasValue()) {
      HashSet<String> set = new HashSet<String>();
      readFileInto(file.getValue(), set);
      return Maybe.<Set<String>>just(set);
    } else {
      return Maybe.<Set<String>>nothing();
    }
  }

  public Maybe<File> getWorldFile() {
    File file = getFile("world", null);
    return Maybe.<File>fromMaybeNull(file);
  }

  public Maybe<Set<String>> getWorld() {
    Maybe<File> file = getWorldFile();
    if (file.hasValue()) {
      HashSet<String> set = new HashSet<String>();
      readFileInto(file.getValue(), set);
      return Maybe.<Set<String>>just(set);
    } else {
      return Maybe.<Set<String>>nothing();
    }
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

  public long getLoggingDepth() {
    return getLong("loggingdepth", -1);
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

              "OrderWrite",
              "OrderRead",

              "ReadObject",
              "ReadPrimitive",
              "WriteObject",
              "WritePrimitive",

              "YieldObject",

              "Branch",

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

  public static void readFileInto(File file, Collection<String> lines) {
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(file);
      br = new BufferedReader(fr);
      String line = null;
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fr != null) fr.close();
        if (br != null) br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
