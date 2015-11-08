/*
 * Copyright (C) 2003-2015 Syed Asad Rahman <asad @ ebi.ac.uk>.
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

package uk.ac.ebi.reactionblast.mapping.algorithm;
//~--- non-JDK imports --------------------------------------------------------

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;
import uk.ac.ebi.reactionblast.mapping.container.MoleculeMoleculeMapping;
import uk.ac.ebi.reactionblast.mapping.interfaces.IGameTheory;
import uk.ac.ebi.reactionblast.mapping.interfaces.IMappingAlgorithm;

/**
 * @contact Syed Asad Rahman, EMBL-EBI, Cambridge, UK.
 * @author Syed Asad Rahman <asad @ ebi.ac.uk>
 */
public final class CalculationProcess extends IsomeraseHandler implements Serializable {

    private final static ILoggingTool logger
            = LoggingToolFactory.createLoggingTool(CalculationProcess.class);
    private static final long serialVersionUID = 0x4a0bba049L;
    private final boolean removeHydrogen;
    private int delta = 0;
    private MoleculeMoleculeMapping reactionBlastMolMapping;
    private final IMappingAlgorithm algorithm;

    /**
     *
     * @param removeHydrogen
     * @param reaction
     * @param algorithm
     */
    public CalculationProcess(
            boolean removeHydrogen,
            IReaction reaction,
            IMappingAlgorithm algorithm) {

        /*
         * This case handles rings cases where 6 membered ring reduces to 5 membered rings Example KEGG reaction R01432
         * of Isomerase class
         */
        super(reaction);

//        System.out.println("I am CalculationProcess");
        this.removeHydrogen = removeHydrogen;
        logger.debug("\n|++++++++++++++++++++++++++++|");
        logger.debug("Performing Atom-Atom Mapping ....... " + reaction.getID() + " .......");
        logger.debug("\n|++++++++++++++++++++++++++++|");
        this.algorithm = algorithm;
        run();
    }

    private synchronized void run() {
        switch (algorithm) {
            case MIN:
                logger.debug("Processing Reaction for Local Minimum: ");
                delta = (int) calRelation(reaction, IMappingAlgorithm.MIN);
                break;
            case MAX:
                logger.debug("Processing Reaction for Global Minimum: ");
                delta = (int) calRelation(reaction, IMappingAlgorithm.MAX);
                break;
            case MIXTURE:
                logger.debug("Processing Reaction for Max-Mixture Model: ");
                delta = (int) calRelation(reaction, IMappingAlgorithm.MIXTURE);
                break;
            case RINGS:
                logger.debug("Processing Reaction for Ring Model: ");
                delta = (int) calRelation(reaction, IMappingAlgorithm.RINGS);
                break;
        }
    }

    public synchronized IReaction getMappedReaction() {
        return reaction;
    }

    private synchronized double calRelation(IReaction reaction, IMappingAlgorithm theory) {
        try {
            Map<Integer, IAtomContainer> educts
                    = Collections.synchronizedSortedMap(new TreeMap<Integer, IAtomContainer>());
            for (int i = 0; i < reaction.getReactantCount(); i++) {
                educts.put(i, reaction.getReactants().getAtomContainer(i));
            }

            Map<Integer, IAtomContainer> products
                    = Collections.synchronizedSortedMap(new TreeMap<Integer, IAtomContainer>());
            for (int i = 0; i < reaction.getProductCount(); i++) {
                products.put(i, reaction.getProducts().getAtomContainer(i));
            }

            GameTheoryMatrix EDSH
                    = new GameTheoryMatrix(theory, reaction, educts, products, removeHydrogen);

            IGameTheory gameTheory = GameTheoryFactory.make(theory,
                    reaction,
                    removeHydrogen,
                    educts,
                    products,
                    EDSH);

            this.reactionBlastMolMapping = gameTheory.getReactionMolMapping();
            EDSH.Clear();

            return gameTheory.getDelta();
        } catch (Exception e) {
            logger.error(e);
            return -1;
        }
    }

    /**
     * @return the delta
     */
    public synchronized int getDelta() {
        return delta;
    }

    /**
     * @return the reactionBlastMolMapping
     */
    public synchronized MoleculeMoleculeMapping getReactionBlastMolMapping() {
        return reactionBlastMolMapping;
    }
    private static final Logger LOG = Logger.getLogger(CalculationProcess.class.getName());
}
