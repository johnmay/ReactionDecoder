/* Copyright (C) 2006-2011  Syed Asad Rahman <asad @ ebi.ac.uk>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed inputStream the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package uk.ac.ebi.reactionblast.tools.rxnfile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.annotations.TestMethod;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.io.IChemObjectReader.Mode;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.MoleculeSetManipulator;
import uk.ac.ebi.reactionblast.tools.ExtAtomContainerManipulator;

/**
 * Class that reads MDL files (various versions). 
 *  
 * 
 *
 * @author Syed Asad Rahman <asad @ ebi.ac.uk>
 */
public class MDLFileReader {

    private static IAtomContainer molecule = null;

    /**
     * Constructor for MDL file reader
     *
     * @param inputStream
     * @param mode
     * @throws IOException
     */
    @TestMethod("MDLFileReaderTest")
    public MDLFileReader(InputStream inputStream, Mode mode) throws IOException {

        try {
            try (MDLV2000Reader reader2 = new MDLV2000Reader(inputStream, mode)) {
                molecule = reader2.read(new AtomContainer());
                reader2.close();
            }
        } catch (CDKException e) {
            String string = e.toString();
            if (string.contains("This file must be read with the MDLV3000Reader.")) {
                try {
                    try (MDLV3000Reader reader2 = new MDLV3000Reader(inputStream, mode)) {
                        molecule = reader2.read(new AtomContainer());
                        reader2.close();
                    }
                } catch (CDKException ex) {
                    Logger.getLogger(MDLFileReader.class.getName()).log(Level.SEVERE, null, ex);
                }

            } else if (string.contains("This file must be read with the MDLReader.")) {
                try {
                    try (MDLV2000Reader reader2 = new MDLV2000Reader(inputStream, mode)) {
                        molecule = reader2.read(new AtomContainer());
                        reader2.close();
                    }
                } catch (CDKException ex) {
                    Logger.getLogger(MDLFileReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Constructor for MDL file reader
     *
     * @param inputStream
     * @throws IOException
     * @throws CDKException
     */
    @TestMethod("MDLFileReaderTest")
    public MDLFileReader(InputStream inputStream) throws IOException, CDKException {
        this(inputStream, Mode.RELAXED);
    }

    /**
     * Constructor for MDL file reader
     *
     * @param reader
     * @throws IOException
     * @throws CDKException
     */
    @TestMethod("MDLFileReaderTest")
    public MDLFileReader(Reader reader) throws IOException, CDKException {
        this(reader, Mode.RELAXED);
    }

    /**
     * Constructor for MDL file reader
     *
     * @param reader
     * @param mode
     * @throws IOException
     */
    @TestMethod("MDLFileReaderTest")
    public MDLFileReader(Reader reader, Mode mode) throws IOException {

        try {
            try (MDLV2000Reader reader2 = new MDLV2000Reader(reader, mode)) {
                molecule = reader2.read(new AtomContainer());
                reader2.close();
            }
        } catch (CDKException e) {
            String string = e.toString();
            if (string.contains("This file must be read with the MDLV3000Reader.")) {
                try (MDLV3000Reader reader2 = new MDLV3000Reader(reader, mode)) {
                    try {
                        molecule = reader2.read(new AtomContainer());
                        reader2.close();
                    } catch (CDKException ex) {
                        Logger.getLogger(MDLFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else if (string.contains("This file must be read with the MDLReader.")) {
                try {
                    try (MDLReader reader2 = new MDLReader(reader, mode)) {
                        molecule = reader2.read(new AtomContainer());
                        reader2.close();
                    }
                } catch (CDKException ex) {
                    Logger.getLogger(MDLFileReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Returns read molecule
     *
     * @return read molecule
     */
    @TestMethod("testGetMolecule")
    public IAtomContainer getAtomContainer() {
        return molecule;
    }

    /**
     * Returns moecule with cleaned Layout
     *
     * @return cleaned Layout molecule
     */
    @TestMethod("testGetMoleculeWithLayoutCheck")
    public IAtomContainer getMoleculeWithLayoutCheck() {
        if (!GeometryTools.has2DCoordinates(molecule)) {
            try {
                StructureDiagramGenerator sdg = new StructureDiagramGenerator(new AtomContainer(molecule));
                sdg.generateCoordinates();
                molecule = sdg.getMolecule();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return molecule;
    }

    /**
     * Returns molecule with new layout
     *
     * @return cleaned model
     */
    @TestMethod("testGetChemModelWithMoleculeWithLayoutCheck")
    public IChemModel getChemModelWithMoleculeWithLayoutCheck() {
        IChemModel chemModel = new ChemModel();
        chemModel.setMoleculeSet(ConnectivityChecker.partitionIntoMolecules(molecule));
        for (IAtomContainer ac : MoleculeSetManipulator.getAllAtomContainers(chemModel.getMoleculeSet())) {
            if (!GeometryTools.has2DCoordinates(ac)) {
                try {
                    IAtomContainer mol = ac.getBuilder().newInstance(IAtomContainer.class, ac);
                    ExtAtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                    StructureDiagramGenerator sdg = new StructureDiagramGenerator(new AtomContainer(mol));
                    sdg.generateCoordinates();
                    ac = sdg.getMolecule();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return chemModel;
    }
    private static final Logger LOG = Logger.getLogger(MDLFileReader.class.getName());
}
