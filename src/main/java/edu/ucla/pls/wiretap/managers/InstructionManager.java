package edu.ucla.pls.wiretap.managers;

import java.util.List;

import edu.ucla.pls.wiretap.utils.Pair;
import edu.ucla.pls.wiretap.WiretapProperties;
import edu.ucla.pls.wiretap.Agent;

public class InstructionManager extends Manager<Pair<Method, Integer>, Instruction> {

  public InstructionManager (WiretapProperties properties, List<Instruction> instructions) {
    super(properties.getInstructionFile(), instructions);
  }

  public InstructionManager (WiretapProperties properties) {
    super(properties.getInstructionFile());
  }

}
