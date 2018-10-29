/*
 * Copyright (C) 2014-2018 Syed Asad Rahman <asad at ebi.ac.uk>.
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
package org.openscience.smsd.algorithm.vflib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.algorithm.mcsplus.ExtractMapping;
import org.openscience.smsd.algorithm.rgraph.CDKRMapHandler;
import org.openscience.smsd.graph.EdgeProductGraph;
import org.openscience.smsd.graph.EdgeType;
import org.openscience.smsd.graph.Graph;
import org.openscience.smsd.graph.IClique;
import org.openscience.smsd.graph.Vertex;
import org.openscience.smsd.graph.algorithm.GraphKoch;
import org.openscience.smsd.interfaces.Algorithm;

/**
 * This class should be used to find MCS between source graph and target graph.
 *
 * First the algorithm runs VF lib
 * {@link org.openscience.smsd.algorithm.vflib.VF2MCS} and reports MCS between
 * run source and target graphs. Then these solutions are extended using
 * McGregor {@link org.openscience.smsd.algorithm.mcgregor.McGregor} algorithm
 * where ever required.
 *
 *
 *
 *
 * @author Syed Asad Rahman <asad at ebi.ac.uk>
 */
public class MCSSeedGenerator implements Callable<List<AtomAtomMapping>> {

    private final boolean DEBUG = false;
    private final IAtomContainer source;
    private final IAtomContainer target;
    private final List<AtomAtomMapping> allCliqueAtomMCS;
    private final boolean shouldMatchRings;
    private final Algorithm algorithm;
    private final static ILoggingTool LOGGER
            = LoggingToolFactory.createLoggingTool(MCSSeedGenerator.class);
    private final boolean shouldMatchBonds;
    private final boolean matchAtomType;

    /**
     *
     * @param source
     * @param target
     * @param bondMatch
     * @param ringMatch
     * @param matchAtomType
     * @param algorithm
     */
    public MCSSeedGenerator(IAtomContainer source, IAtomContainer target, boolean bondMatch, boolean ringMatch, boolean matchAtomType, Algorithm algorithm) {
        this.source = source;
        this.target = target;
        this.allCliqueAtomMCS = new ArrayList<>();
        this.shouldMatchRings = ringMatch;
        this.algorithm = algorithm;
        this.matchAtomType = matchAtomType;
        this.shouldMatchBonds = bondMatch;
    }

    public MCSSeedGenerator(IQueryAtomContainer source, IAtomContainer target, Algorithm algorithm) {
        this.source = source;
        this.target = target;
        this.allCliqueAtomMCS = new ArrayList<>();
        this.shouldMatchRings = true;
        this.algorithm = algorithm;
        this.matchAtomType = true;
        this.shouldMatchBonds = true;
    }

    @Override
    public List<AtomAtomMapping> call() throws Exception {
        if (DEBUG) {
            System.out.println("ac1: " + this.source.getAtomCount());
            System.out.println("ac2: " + this.target.getAtomCount());
        }
        switch (algorithm) {
            case CDKMCS:
                if (DEBUG) {
                    System.out.println("Calling CDKMCS for seeding " + shouldMatchBonds + " " + shouldMatchRings);
                }
                List<AtomAtomMapping> addUIT = addUIT();
                if (DEBUG) {
                    System.out.println("addUIT " + addUIT.iterator().next().getCount());
                }
                return addUIT;
            case MCSPlus:
                if (DEBUG) {
                    System.out.println("Calling MCSPLUS for seeding " + shouldMatchBonds + " " + shouldMatchRings);
                }
                List<AtomAtomMapping> addKochCliques = addKochCliques();
                if (DEBUG) {
                    System.out.println("MCSPLUS " + addKochCliques.iterator().next().getCount());
                }
                return addKochCliques;
            default:
                return Collections.unmodifiableList(allCliqueAtomMCS);
        }
    }

    protected synchronized List<AtomAtomMapping> addKochCliques() throws IOException {
        IAtomContainer ac1;
        IAtomContainer ac2;
        boolean flagExchange = false;

        if (source instanceof IQueryAtomContainer) {
            ac1 = (IQueryAtomContainer) source;
            ac2 = target;
        } else if (source.getAtomCount() <= target.getAtomCount()) {
            ac1 = source;
            ac2 = target;
        } else {
            flagExchange = true;
            ac1 = target;
            ac2 = source;
        }
        if (DEBUG) {
            System.out.println("Starting GenerateCompatibilityGraph");
        }

        EdgeProductGraph gcg
                = new EdgeProductGraph(ac1, ac2, shouldMatchBonds, shouldMatchRings, matchAtomType);
        int search_cliques = gcg.searchCliques();
        Graph comp_graph_nodes = gcg.getCompatibilityGraph();
        if (DEBUG) {
            System.out.println("**************************************************");
            System.out.println("--Compatibility Graph--");
            System.out.println("C_edges: " + comp_graph_nodes.getEdgesOfType(EdgeType.C_EDGE).size());
            System.out.println("D_edges: " + comp_graph_nodes.getEdgesOfType(EdgeType.D_EDGE).size());
            System.out.println("Vertices: " + comp_graph_nodes.V());
            System.out.println("Edges: " + comp_graph_nodes.E());
            System.out.println("**************************************************");
        }

        IClique init = null;
        init = new GraphKoch(comp_graph_nodes);
        init.findMaximalCliques();

        Stack<Set<Vertex>> maxCliqueSet = init.getMaxCliquesSet();
        if (DEBUG) {
            System.out.println("Max_Cliques_Set: " + maxCliqueSet.size());
            System.out.println("**************************************************");
        }
        List<Map<Integer, Integer>> mappings = new ArrayList<>();

        while (!maxCliqueSet.empty()) {
            Map<Integer, Integer> indexindexMapping;
            indexindexMapping = ExtractMapping.getMapping(
                    comp_graph_nodes, ac1, ac2, maxCliqueSet.peek(), shouldMatchRings, matchAtomType);
            if (indexindexMapping != null) {
                mappings.add(indexindexMapping);
//                    if (DEBUG) {
//                        System.out.println("mappings " + mappings);
//                    }
            }
            maxCliqueSet.pop();
        }

        for (Map<Integer, Integer> peek : mappings) {
            AtomAtomMapping atomatomMapping = new AtomAtomMapping(source, target);

            for (Map.Entry<Integer, Integer> map : peek.entrySet()) {
                Integer qIndex = map.getKey();
                Integer tIndex = map.getValue();
                if (qIndex != -1 && tIndex != -1) {
                    IAtom qAtom;
                    IAtom tAtom;
                    if (flagExchange) {
                        qAtom = source.getAtom(tIndex);
                        tAtom = target.getAtom(qIndex);
                    } else {
                        qAtom = source.getAtom(qIndex);
                        tAtom = target.getAtom(tIndex);
                    }
                    atomatomMapping.put(qAtom, tAtom);
                } else {
                    try {
                        throw new CDKException("Atom index pointing to -1");
                    } catch (CDKException ex) {
                        LOGGER.error(Level.SEVERE, null, ex);
                    }
                }
            }

            if (!atomatomMapping.isEmpty()) {
                allCliqueAtomMCS.add(atomatomMapping);
            }
        }
        gcg.clear();
//        mergedResult.clear();
        return Collections.unmodifiableList(allCliqueAtomMCS);
    }

    /**
     *
     * @return
     */
    private List<AtomAtomMapping> addUIT() throws CDKException {
        CDKRMapHandler rmap = new CDKRMapHandler();
        List<Map<Integer, Integer>> solutions;
        boolean rOnPFlag;
        if (source instanceof IQueryAtomContainer) {
            rOnPFlag = false;
            solutions = rmap.calculateOverlapsAndReduce(target, (IQueryAtomContainer) source);
        } else if (source.getAtomCount() > target.getAtomCount()) {
            rOnPFlag = true;
            solutions = rmap.calculateOverlapsAndReduce(source, target, shouldMatchBonds, shouldMatchRings, true);
        } else {
            rOnPFlag = false;
            solutions = rmap.calculateOverlapsAndReduce(target, source, shouldMatchBonds, shouldMatchRings, true);
        }
        return setUITMappings(rOnPFlag, solutions);
    }

    private List<AtomAtomMapping> setUITMappings(boolean RONP, List<Map<Integer, Integer>> sol) {
        /*
         * Sort biggest clique to smallest
         */
        Collections.sort(sol, new Map1ValueComparator(SortOrder.DESCENDING));
        sol.stream().map((Map<Integer, Integer> solution) -> {
            AtomAtomMapping atomatomMapping = new AtomAtomMapping(source, target);
            solution.keySet().stream().forEach((qAtomIndex) -> {
                IAtom qAtom;
                IAtom tAtom;
                int qIndex;
                int tIndex;

                if (RONP) {
                    qAtom = source.getAtom(qAtomIndex);
                    tAtom = target.getAtom(solution.get(qAtomIndex));
                } else {
                    tAtom = target.getAtom(qAtomIndex);
                    qAtom = source.getAtom(solution.get(qAtomIndex));
                }

                qIndex = source.indexOf(qAtom);
                tIndex = target.indexOf(tAtom);
                if (qIndex != -1 && tIndex != -1) {
                    atomatomMapping.put(qAtom, tAtom);
                } else {
                    try {
                        throw new CDKException("Atom index pointing to -1");
                    } catch (CDKException ex) {
                        LOGGER.error(Level.SEVERE, null, ex);
                    }
                }
            });
            return atomatomMapping;
        }).filter((atomatomMapping) -> (!atomatomMapping.isEmpty())).forEach((atomatomMapping) -> {
            allCliqueAtomMCS.add(atomatomMapping);
        });
        return Collections.unmodifiableList(allCliqueAtomMCS);
    }

    private int[] getIndex(int cliqueIndex, List<Integer> comp_graph_nodes) {
        int[] v = new int[2];
        v[0] = -1;
        v[1] = -1;
        for (int i = 0; i < comp_graph_nodes.size(); i += 3) {
            if (cliqueIndex == comp_graph_nodes.get(i + 2)) {
                v[0] = comp_graph_nodes.get(i);
                v[1] = comp_graph_nodes.get(i + 1);
            }
        }
        return v;
    }
}
