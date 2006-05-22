/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.data.append.row;

import java.util.Iterator;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultCellIterator;

/**
 * A row that takes a base row and resorts the cells in it according to 
 * and <code>int[]</code> parameter passed in the constructor. 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ResortedCellsRow implements DataRow {
    
    private final DataRow m_row;
    private final int[] m_sort;
    
    /**
     * Creates new row with <code>row</code> as underlying base row and
     * <code>sort</code> the new sorting scheme. That is the old 
     * <code>i</code>-th entry becomes entry number <code>sort[i]</code>.
     * @param row The base row.
     * @param sort The resorting.
     * @throws IllegalArgumentException If length of arrays don't match.
     * @throws NullPointerException If either argument is <code>null</code>.
     */
    protected ResortedCellsRow(final DataRow row, final int[] sort) {
        if (row.getNumCells() != sort.length) {
            throw new IllegalArgumentException("Length don't match.");
        }
        m_row = row;
        m_sort = sort;
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_row.getNumCells();
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getCell(int)
     */
    public DataCell getCell(final int index) {
        return m_row.getCell(m_sort[index]);
    }
    
    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

}
