/*
 * Copyright (C) 2007-2015 Syed Asad Rahman <asad @ ebi.ac.uk>.
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
package uk.ac.ebi.reactionblast.stereo.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IMapping;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.tools.manipulator.MoleculeSetManipulator;
import uk.ac.ebi.reactionblast.stereo.wedge.WedgeStereoAnalyser;
import uk.ac.ebi.reactionblast.stereo.wedge.WedgeStereoAnalysisResult;
import uk.ac.ebi.reactionblast.stereo.wedge.WedgeStereoLifter;

/**
 * Tool to check mapped pairs of atoms, to see if they have compatible stereo wedges.
 *
 * @author maclean
 *
 */
public class WedgeStereoComparisonTool {

    public static List<WedgeStereoComparisonResult> compare(IReaction reaction) {
        List<WedgeStereoComparisonResult> results
                = new ArrayList<>();

        WedgeStereoLifter lifter = new WedgeStereoLifter();
        IAtomContainerSet reactants = reaction.getReactants();
        IAtomContainerSet products = reaction.getProducts();
        for (IMapping mapping : reaction.mappings()) {
            IAtom atomA = (IAtom) mapping.getChemObject(0);
            IAtom atomB = (IAtom) mapping.getChemObject(1);
            IAtomContainer atomContainerA = MoleculeSetManipulator.getRelevantAtomContainer(reactants, atomA);
            IAtomContainer atomContainerB = MoleculeSetManipulator.getRelevantAtomContainer(products, atomB);
            results.add(compare(atomA, atomContainerA, atomB, atomContainerB, lifter));
        }
        return results;
    }

    /**
     * Compare a (mapped) pair of atom containers to check that they have the same stereo centers.
     *
     * @param atomContainerA an atom container
     * @param atomContainerB another atom container
     * @param equivMap mapped atoms between the pair
     * @return
     */
    public static List<WedgeStereoComparisonResult> compare(
            IAtomContainer atomContainerA, IAtomContainer atomContainerB,
            Map<Integer, Integer> equivMap) {
        List<WedgeStereoComparisonResult> results
                = new ArrayList<>();

        WedgeStereoLifter lifter = new WedgeStereoLifter();
        for (int indexA = 0; indexA < atomContainerA.getAtomCount(); indexA++) {
            int indexB = equivMap.get(indexA);
            IAtom atomA = atomContainerA.getAtom(indexA);
            IAtom atomB = atomContainerB.getAtom(indexB);
            results.add(compare(atomA, atomContainerA, atomB, atomContainerB, lifter));
        }

        return results;
    }

    public static WedgeStereoComparisonResult compare(
            IAtom atomA, IAtomContainer atomContainerA,
            IAtom atomB, IAtomContainer atomContainerB,
            WedgeStereoLifter lifter) {

        WedgeStereoAnalysisResult resultForA = WedgeStereoAnalyser.getResult(atomA, atomContainerA, lifter);
        WedgeStereoAnalysisResult resultForB = WedgeStereoAnalyser.getResult(atomB, atomContainerB, lifter);

        return new WedgeStereoComparisonResult(
                atomA, atomContainerA, resultForA, atomB, atomContainerB, resultForB);
    }
    private static final Logger LOG = Logger.getLogger(WedgeStereoComparisonTool.class.getName());
}
