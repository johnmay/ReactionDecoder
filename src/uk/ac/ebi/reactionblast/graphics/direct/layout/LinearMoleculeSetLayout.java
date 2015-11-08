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
package uk.ac.ebi.reactionblast.graphics.direct.layout;

import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import uk.ac.ebi.reactionblast.graphics.direct.Params;

public class LinearMoleculeSetLayout extends AbstractDirectLayout<IAtomContainerSet> {

    /**
     * This is an axis for the individual molecules to be aligned to
     */
    private Vector2d moleculeAxis;

    public LinearMoleculeSetLayout(Params params) {
        this(params, true);
    }

    public LinearMoleculeSetLayout(Params params, boolean shouldInvert) {
        this(params, shouldInvert, new Vector2d(1, 0));
    }

    public LinearMoleculeSetLayout(
            Params params, boolean shouldInvert, Vector2d moleculeAxis) {
        super(shouldInvert);
        this.moleculeAxis = moleculeAxis;
        setParams(params);
    }

    @Override
    public BoundsTree layout(IAtomContainerSet atomContainerSet, Vector2d moleculeSetAxis) {
        int bondLength = params.bondLength;
        int molGap = 2 * params.plusGap;

        // if the molecules don't have labels, need to label them
        int molLabel = 0;

        String rootLabel = atomContainerSet.getID();
        boundsTree = new BoundsTree(rootLabel);
        Point2d curr = new Point2d(0, 0);
        int i = 0;
        for (IAtomContainer molecule : atomContainerSet.atomContainers()) {
            if (!GeometryTools.has2DCoordinates(molecule)) {
                //Added by Asad for 3D to 2D

                StructureDiagramGenerator sdg
                        = new StructureDiagramGenerator(new AtomContainer(molecule));
                try {
                    sdg.generateCoordinates();
                } catch (CDKException ex) {
                    Logger.getLogger(LinearMoleculeSetLayout.class.getName()).log(Level.SEVERE, null, ex);
                }
                molecule = sdg.getMolecule();

            }
            invert(molecule);
            if (params.alignMolecules && moleculeAxis != null) {
                align(molecule, moleculeAxis);
            }
            GeometryTools.scaleMolecule(molecule,
                    GeometryTools.getScaleFactor(molecule, bondLength));
            Rectangle2D bounds = GeometryTools.getRectangle2D(molecule);

            double boundsWidth = bounds.getWidth();
            double halfBoundsWidth = boundsWidth / 2;

            curr.scaleAdd(halfBoundsWidth, moleculeSetAxis, curr);
            translateTo(molecule, curr.x, curr.y, bounds);
            curr.scaleAdd(halfBoundsWidth, moleculeSetAxis, curr);
            curr.scaleAdd(molGap, moleculeSetAxis, curr);

            String moleculeLabel = molecule.getID();
            if (moleculeLabel == null || moleculeLabel.equals("")) {
                moleculeLabel = "mol" + String.valueOf(molLabel);
                molLabel++;
            } else {
                moleculeLabel += ":" + i;
            }

            boundsTree.add(rootLabel + "_" + moleculeLabel, bounds);
            i++;
            shouldInvert = true;
        }
        return boundsTree;
    }

    @Override
    public Vector2d getAxis() {
        return new Vector2d(1, 0);
    }

    @Override
    public double getAxisPosition() {
        return (boundsTree.getWidth() / 2) + params.borderX;
    }
    private static final Logger LOG = Logger.getLogger(LinearMoleculeSetLayout.class.getName());
}
