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
package uk.ac.ebi.reactionblast.mapping.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMapping;

/**
 * An association between two subgraphs (or 'blocks') of the reaction.
 *
 * @author maclean
 *
 */
public class BlockPair {

    private final Block reactantBlock;

    private final Block productBlock;

    private final List<IMapping> mappings;

    public BlockPair(IAtomContainer reactant, IAtomContainer product) {
        mappings = new ArrayList<IMapping>();
        reactantBlock = new Block(reactant);
        productBlock = new Block(product);
        reactantBlock.setPartner(productBlock);
        productBlock.setPartner(reactantBlock);
    }

    public void addMapping(IMapping mapping, IAtom rAtom, IAtom pAtom) {
        mappings.add(mapping);
        reactantBlock.addMapping(rAtom, pAtom);
        productBlock.addMapping(pAtom, rAtom);
    }

    public Block getReactantBlock() {
        return reactantBlock;
    }

    public Block getProductBlock() {
        return productBlock;
    }

    @Override
    public String toString() {
        return reactantBlock + "-" + productBlock;
    }
    private static final Logger LOG = Logger.getLogger(BlockPair.class.getName());

}
