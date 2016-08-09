package edu.ucla.pls.wiretap;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;


public class Properties {

  public final File folder;
  public final Collection<String> ignoredPrefixes;

  public final File classfile;
  public final File methodfile;

  public final File classesfolder;
  public final File logfolder;

	/**
	*
	*/
	public Properties(File folder, Collection<String> ignoredPrefixes) {
    this.folder = folder;
    this.ignoredPrefixes = ignoredPrefixes;

    this.classfile = new File(folder, "classes.txt");
    this.methodfile = new File(folder, "methods.txt");

    this.classesfolder = new File(folder, "classes/");
    this.logfolder = new File(folder, "log/");
	}

  public boolean doDumpClassFiles() {
    return classesfolder != null;
  }

  public boolean isIgnored(String className) {
    for (String prefix : ignoredPrefixes) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  public void print(PrintStream out) {
    out.println(String.format("folder     = '%s'", this.folder));
    out.println(String.format("classfile  = '%s'", this.classfile));
    out.println(String.format("methodfile = '%s'", this.methodfile));
  }

	public static Properties fromFile(File properties) {
    throw new UnsupportedOperationException();
  }



}

