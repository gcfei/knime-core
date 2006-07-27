/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 23, 2005 (wiswedel): created
 *   21.06.06 (bw & po): reviewed
 */
package de.unikn.knime.core.data.def;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DoubleValue;

/** Test the default cell iterator class.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultCellIteratorTest extends TestCase {
    
    /** Test all methods in iterator. */
    public void testIterator() {
        double[] d = new double[]{1.0, 2.0, 3.0};
        DataRow row = new DefaultRow(new StringCell("Key"), d);
        Iterator<DataCell> it = row.iterator();
        int i = 0;
        while (it.hasNext()) {
            DataCell cell = it.next();
            DoubleValue dCell = (DoubleValue)cell;
            assertEquals(dCell.getDoubleValue(), d[i]);
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException uoe) {
                System.out.println("Caught UnsupportedOperationException: " 
                        + uoe.getMessage());
            }
            i++;
        }
        assertFalse(it.hasNext());
        assertEquals(i, d.length);
        try {
            it.next();
            fail();
        } catch (NoSuchElementException nse) {
            System.out.println("Caught NoSuchElementException: " 
                    + nse.getMessage());
        }
    }

}
