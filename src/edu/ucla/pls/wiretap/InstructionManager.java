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

import edu.ucla.pls.utils.Pair;

public class InstructionManager implements Closeable {

  public final WiretapProperties properties;
  public final List<Instruction> instructions;

  public final Map<Pair<Method, Integer>, Instruction> lookup;

  private Writer writer;

  public InstructionManager (WiretapProperties properties, List<Instruction> instructions) {
    this.properties = properties;
    this.instructions = instructions;
    this.lookup = new HashMap<Pair<Method, Integer>, Instruction> ();

    for (Instruction inst: instructions) {
      lookup.put(inst, inst);
    }
  }

  public InstructionManager (WiretapProperties properties) {
    this(properties, new ArrayList<Instruction>());
  }

  public void setup() {
    final File instfile = properties.getInstructionFile();
    try {
      writer = new BufferedWriter(new FileWriter(instfile));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public Instruction getInstruction(Method m, int offset)  {
    final Pair key = Pair.of(m, Integer.valueOf(offset));
    final Instruction inst = getInstructionUnsafe(key);
    return inst != null ? inst : createInstruction(key);
  }

  private synchronized Instruction createInstruction(Pair<Method, Integer> key) {
    Instruction inst = new Instruction(instructions.size(), key.fst, key.snd);
    instructions.add(inst);
    lookup.put(inst, inst);
    try {
      writer.write(inst.toString());
      writer.write("\n");
    } catch (IOException e) {
      System.err.println("Could not write '" + inst + "' to file");
    }
    return inst;
  }


  public synchronized Instruction getInstruction(int id)  {
    return instructions.get(id);
  }

  public synchronized Instruction getInstructionUnsafe(Pair<Method, Integer> key)  {
    return lookup.get(key);
  }

	@Override
	public void close() throws IOException {
    writer.close();
	}

}
