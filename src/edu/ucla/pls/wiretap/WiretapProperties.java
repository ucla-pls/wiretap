package edu.ucla.pls.wiretap;

import java.io.File;
import java.io.PrintStream;
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

  public Collection<String> getIgnoredPrefixes() {
    if (ignoredPrefixes == null) {
      ignoredPrefixes = getList("ignoredprefixes", Collections.<String>emptyList());
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
    return Maybe.<File>fromMaybeNull(getFile("classfilesfolder", null));
  }

  public boolean doDumpClassFiles() {
    return getClassFilesFolder().hasValue();
  }

  @Override
  public synchronized Object setProperty(String key, String value) {
    throw new UnsupportedOperationException("WiretapProperties are read-only");
  }

  @Override
  public String getProperty(String key, String def) {
    return super.getProperty("wiretap." + key, def);
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
