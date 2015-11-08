/*
 * Copyright (C) 2007-2015 Syed Asad Rahman <asad@ebi.ac.uk>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package generic;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.Reaction;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.smiles.SmilesGenerator;
import uk.ac.ebi.reactionblast.mechanism.ReactionMechanismTool;
import uk.ac.ebi.reactionblast.tools.rxnfile.MDLRXNV2000Reader;

/**
 * @contact Syed Asad Rahman, EMBL-EBI, Cambridge, UK.
 * @author Syed Asad Rahman <asad @ ebi.ac.uk>
 */
public class UnblancedReactionChecker {

    private static final boolean DEBUG = false;
    private static final File dir = new File("rxn/rhea");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String f : list) {
//                System.out.println("F " + f);
                IReaction rxnReactions;
                try (MDLRXNV2000Reader reader = new MDLRXNV2000Reader(new FileReader(new File(dir, f)));) {
                    try {
                        rxnReactions = reader.read(new Reaction());
                        reader.close();
                        rxnReactions.setID(f.split(".rxn")[0]);
                        if (!isBalanced(rxnReactions)) {
                            System.out.println("Unbalanced Reaction " + f);
                        }
                    } catch (IOException | CDKException ex) {
                        System.err.println("ERROR in Reading Reaction file " + f + "\n" + ex);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(UnblancedReactionChecker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static boolean isBalanced(IReaction r) {

        Map<String, Integer> atomUniqueCounter1 = new TreeMap<>();
        Map<String, Integer> atomUniqueCounter2 = new TreeMap<>();

        int leftHandAtomCount = 0;
        for (IAtomContainer q : r.getReactants().atomContainers()) {
            for (IAtom a : q.atoms()) {
                if (a.getSymbol().equals("H")) {
                    continue;
                }
                if (!atomUniqueCounter1.containsKey(a.getSymbol())) {
                    atomUniqueCounter1.put(a.getSymbol(), 1);
                } else {
                    int counter = atomUniqueCounter1.get(a.getSymbol()) + 1;
                    atomUniqueCounter1.put(a.getSymbol(), counter);
                }
                leftHandAtomCount++;
            }
            if (DEBUG) {
                try {
                    System.out.println("Q=mol " + SmilesGenerator.generic().create(q));
                } catch (CDKException ex) {
                    Logger.getLogger(ReactionMechanismTool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        int rightHandAtomCount = 0;
        for (IAtomContainer t : r.getProducts().atomContainers()) {
            for (IAtom b : t.atoms()) {
                if (b.getSymbol().equals("H")) {
                    continue;
                }
                if (!atomUniqueCounter2.containsKey(b.getSymbol())) {
                    atomUniqueCounter2.put(b.getSymbol(), 1);
                } else {
                    int counter = atomUniqueCounter2.get(b.getSymbol()) + 1;
                    atomUniqueCounter2.put(b.getSymbol(), counter);
                }
                rightHandAtomCount++;
            }
            if (DEBUG) {
                try {
                    System.out.println("T=mol " + SmilesGenerator.generic().create(t));
                } catch (CDKException ex) {
                    Logger.getLogger(ReactionMechanismTool.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        if (DEBUG) {
            System.out.println("atomUniqueCounter1 " + leftHandAtomCount);
            System.out.println("atomUniqueCounter2 " + rightHandAtomCount);
        }

        if (leftHandAtomCount != rightHandAtomCount) {
            System.err.println();
            System.err.println("Number of atom(s) on the Left side " + leftHandAtomCount
                    + " =/= Number of atom(s) on the Right side " + rightHandAtomCount);
            for (String s : atomUniqueCounter1.keySet()) {
                if (atomUniqueCounter2.containsKey(s)) {
                    if (atomUniqueCounter1.get(s) != atomUniqueCounter2.get(s).intValue()) {
                        System.err.println(s + "(" + atomUniqueCounter1.get(s) + ")" + " =/= " + s + "(" + atomUniqueCounter2.get(s) + ")");
                    }
                }
                if (!atomUniqueCounter2.containsKey(s)) {
                    System.err.println(s + "(" + atomUniqueCounter1.get(s) + ")" + " =/= " + s + "(" + 0 + ")");
                }
            }
            for (String s : atomUniqueCounter2.keySet()) {
                if (!atomUniqueCounter1.containsKey(s)) {
                    System.err.println(s + "(" + 0 + ")" + " =/= " + s + "(" + atomUniqueCounter2.get(s) + ")");
                }
            }
            return false;
        } else if (!atomUniqueCounter1.keySet().equals(atomUniqueCounter2.keySet())) {
            System.err.println();
            System.err.println("Number of unique atom types(s) on the Left side " + atomUniqueCounter1.size()
                    + " =/= Number of unique atom types(s)on the Right side " + atomUniqueCounter2.size());
            for (String s : atomUniqueCounter1.keySet()) {
                if (atomUniqueCounter2.containsKey(s)) {
                    if (atomUniqueCounter1.get(s) != atomUniqueCounter2.get(s).intValue()) {
                        System.err.println("Number of reactant Atom: " + s + "(" + atomUniqueCounter1.get(s) + ")" + " =/= Number of product atom: " + s + "(" + atomUniqueCounter2.get(s) + ")");
                    }
                }
                if (!atomUniqueCounter2.containsKey(s)) {
                    System.err.println("Number of reactant Atom: " + s + "(" + atomUniqueCounter1.get(s) + ")" + " =/= Number of product atom: " + s + "(" + 0 + ")");
                }
            }
            for (String s : atomUniqueCounter2.keySet()) {
                if (!atomUniqueCounter1.containsKey(s)) {
                    System.err.println("Number of reactant Atom: " + s + "(" + 0 + ")" + " =/= Number of product atom: " + s + "(" + atomUniqueCounter2.get(s) + ")");
                }
            }
            return false;
        }

        if (DEBUG) {
            System.out.println("atomUniqueCounter1 " + atomUniqueCounter1);
            System.out.println("atomUniqueCounter2 " + atomUniqueCounter2);
        }
        return atomUniqueCounter1.keySet().equals(atomUniqueCounter2.keySet());
    }
    private static final Logger LOG = Logger.getLogger(UnblancedReactionChecker.class.getName());

}
