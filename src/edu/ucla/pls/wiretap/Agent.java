package edu.ucla.pls.wiretap;

import java.lang.instrument.Instrumentation;

/**
 * @author Christian Gram Kalhauge <kalhauge@cs.ucla.edu>
 */

public class Agent {


    public static void premain(String options, Instrumentation inst) {
        greet(options);
    }


    private static void greet(String options) {
        System.err.println("== Running program with Wiretap ==");
        if (options != null) System.err.println(options);
        else System.err.println(" Running with default options");
        System.err.println("==================================");
    }
}
