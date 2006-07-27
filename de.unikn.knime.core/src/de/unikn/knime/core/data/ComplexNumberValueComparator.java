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
 *   23.03.2006 (cebron): created
 *   21.06.06 (bw & po): reviewed
 */
package de.unikn.knime.core.data;

/**
 * Comparator returned by the <code>ComplexNumberValue</code>. 
 * 
 * @see de.unikn.knime.core.data.ComplexNumberValue.ComplexNumberUtilityFactory
 * @author ciobaca, Konstanz University
 */
public class ComplexNumberValueComparator extends DataValueComparator {

    /**
     * Compares to <code>ComplexNumberValue</code> based on their real part.
     * @see de.unikn.knime.core.data.DataValueComparator
     *      #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        double real1 = ((ComplexNumberValue)v1).getRealValue();
        double real2 = ((ComplexNumberValue)v2).getRealValue();
        return Double.compare(real1, real2);
    }

}

