/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * History
 *   19.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellComparator;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * A filter selecting rows depending on the content of a column (or cell of the
 * row). The cell content can either be matched against a regular expression or
 * compared to a given range. Matches and rows inside the range can either be
 * included or excluded. RegExpr match can be done case sensitive or
 * insensitive.
 * 
 * @author ohl, University of Konstanz
 */
public class ColValRowFilter extends RowFilter {

    private static final String CFG_COLINDEX = "ColValRowFilterColIndex";

    private static final String CFG_INCLUDE = "ColValRowFilterInclude";

    private static final String CFG_STARTSWITH = "ColValRowFilterStart";

    private static final String CFG_PATTERN = "ColValRowFilterPattern";

    private static final String CFG_CASESENSE = "ColValRowFilterCaseSense";

    private static final String CFG_UPPERBOUND = "ColValRowFilterUpperBound";

    private static final String CFG_LOWERBOUND = "ColValRowFilterLowerBound";

    private boolean m_include;

    private int m_colIndex;

    /*
     * variables for the regExpr matching
     */
    private Pattern m_pattern;

    private boolean m_caseSensitive;

    private boolean m_startsWith;

    /*
     * variables for the range matching
     */

    private DataCell m_lowerBound;

    private DataCell m_upperBound;

    private DataCellComparator m_dcComp;

    /**
     * Creates a new filter which matches the string representation of the
     * specified column against a given regular expression.
     * 
     * @param regExpr a valid regular expression. Causes an exception to fly if
     *            its not a valid reg expr.
     * @param colIndex the index of the cell to test in the row
     * @param include flag indicating whether to include or exclude rows with a
     *            matching value
     * @param caseSensitive if true the match will be done case sensitive, or if
     *            false case insensitive
     * @param startsWith determines wether the value must match entirely the
     *            regular expression or can only start with it.
     * @throws IllegalArgumentException if the pattern passed as regular
     *             epression is not a valid regExpr
     */
    public ColValRowFilter(final String regExpr, final int colIndex,
            final boolean include, final boolean caseSensitive,
            final boolean startsWith) {
        // clear all variables
        this();

        m_colIndex = colIndex;
        m_include = include;
        m_startsWith = startsWith;
        m_caseSensitive = caseSensitive;

        try {
            if (caseSensitive) {
                m_pattern = Pattern.compile(regExpr);
            } else {
                m_pattern = Pattern.compile(regExpr, Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Error in regular expression ('"
                    + pse.getMessage() + "')");
        }
        if (colIndex < 0) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the column index must be a positive number");
        }

    }

    /**
     * Creates a new filter that tests the value of the specified cell against
     * the specified range.
     * 
     * @param comp the comparator that will be used to compare the column's cell
     *            with the bounds of the range.
     * @param lowerBound if the comparator doesn't return a negative number when
     *            the cell is compared with this value, the cell is above the
     *            lower bound. If null no minimum is set.
     * @param upperBound if the comparator doesn't return a positive number when
     *            the cell is compared with this value, the cell is below the
     *            upper bound. If null, no maximum is set.
     * @param colIndex the index of the cell to test in the row
     * @param include determines whether to include or exclude rows with a value
     *            inside the range
     */
    public ColValRowFilter(final DataCellComparator comp,
            final DataCell lowerBound, final DataCell upperBound,
            final int colIndex, final boolean include) {
        // clear all variables
        this();

        m_include = include;
        m_colIndex = colIndex;

        m_dcComp = comp;
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;

        if (comp == null) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the datacell comparator must not be null.");
        }
        if ((upperBound == null) && (lowerBound == null)) {
            throw new IllegalArgumentException("Column value filter: "
                    + "only one of the range bounds can be null.");
        }
        if ((upperBound != null) && (lowerBound != null)) {
            if (comp.compare(m_lowerBound, m_upperBound) > 0) {
                throw new IllegalArgumentException(
                        "Column value filter: the lower bound of the range "
                                + "must not be larger than the upper bound");
            }
        }
        if (colIndex < 0) {
            throw new IllegalArgumentException("Column value filter: "
                    + "the column index must be a positive number");
        }
    }

    /**
     * Default contructor. Don't use without loading settings before.
     */
    public ColValRowFilter() {
        m_include = false;
        m_colIndex = -1;
        m_pattern = null;
        m_caseSensitive = false;
        m_startsWith = false;
        m_lowerBound = null;
        m_upperBound = null;
        m_dcComp = null;
    }

    /**
     * sets a new comparator used to check the range if lower and upper bounds
     * are set. This MUST be called after settings have been loaded from a
     * config object and upper/lower bounds were set. It is always save to set a
     * comparator. Its only used if at least one bound is set.
     * 
     * @param dcComp the comparator used to compare the column's value with the
     *            upper and lower range.
     */
    public void setDataValueComparator(final DataCellComparator dcComp) {
        m_dcComp = dcComp;
    }

    /**
     * @return true if rows inside the specified range or matching the specified
     *         regular expression will be included, false if they are excluded.
     */
    public boolean includeMatchingLines() {
        return m_include;
    }

    /**
     * @return the index of the column whose value is tested.
     */
    public int getColumnIndex() {
        return m_colIndex;
    }

    /**
     * @return true if the range of the column is tested.
     */
    public boolean rangeSet() {
        // if we have a comparator and at least one bound is specified
        return ((m_lowerBound != null) || (m_upperBound != null));
    }

    /**
     * @return the lower bound of the range the value of the specified column is
     *         tested against. Null if no range testing is happening or if no
     *         minimum is set.
     */
    public DataCell getLowerBound() {
        if (rangeSet()) {
            return m_lowerBound;
        } else {
            return null;
        }
    }

    /**
     * @return the upper bound of the range the value of the specified column is
     *         tested against. Null if no range testing is happening or if no
     *         maximum is set.
     */
    public DataCell getUpperBound() {
        if (rangeSet()) {
            return m_upperBound;
        } else {
            return null;
        }
    }

    /**
     * @return true if the string representation of the column's value is tested
     *         against a regular expression.
     */
    public boolean testingStringPattern() {
        return (m_pattern != null);
    }

    /**
     * @return the reg expr the column's value is tested against, or null if no
     *         pattern matching is happening.
     */
    public String getRegExpr() {
        if (m_pattern != null) {
            return m_pattern.pattern();
        } else {
            return null;
        }
    }

    /**
     * @return true if the value pattern must entirely match the reg expr, false
     *         if it can also only start with the expression.
     */
    public boolean mustEntirelyMatch() {
        return !m_startsWith;
    }

    /**
     * @return true if the reg expr match is done in a case sensitive way.
     */
    public boolean caseSensitiveMatch() {
        return m_caseSensitive;
    }

    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #matches(de.unikn.knime.core.data.DataRow, int)
     */
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {

        /*
         * if this goes off you propably didn't set a comparator after loading a
         * range from a config object. Which is not good.
         */
        assert (((m_lowerBound == null) 
                && (m_upperBound == null)) || m_dcComp != null);

        DataCell theCell = row.getCell(m_colIndex);
        boolean match = false;

        if (theCell.isMissing()) {
            // missing cells never match
            return false;
        }

        // do a range checking if we have a comparator and at least one bound
        if (rangeSet() && (m_dcComp != null)) {
            if (m_lowerBound != null) {
                match = (m_dcComp.compare(m_lowerBound, theCell) <= 0);
            } else {
                // if no lowerBound is specified - its always above the minimum
                match = true;
            }
            if (m_upperBound != null) {
                match &= (m_dcComp.compare(theCell, m_upperBound) <= 0);
            }
        }
        if (m_pattern != null) {

            Matcher matcher = m_pattern.matcher(theCell.toString());
            if (m_startsWith) {
                match = matcher.lookingAt();
            } else {
                match = matcher.matches();
            }
        }
        return ((m_include && match) || (!m_include && !match));
    }

    /**
     * A comparator MUST be set if a range is specified in the config object!!
     * 
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #loadSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    public void loadSettingsFrom(final NodeSettings cfg)
            throws InvalidSettingsException {

        m_colIndex = cfg.getInt(CFG_COLINDEX);
        if (m_colIndex < 0) {
            throw new InvalidSettingsException("Column value filter: "
                    + "NodeSettings object contains invalid column index");
        }
        m_include = cfg.getBoolean(CFG_INCLUDE);

        m_caseSensitive = cfg.getBoolean(CFG_CASESENSE);
        m_startsWith = cfg.getBoolean(CFG_STARTSWITH);
        String regExpr = cfg.getString(CFG_PATTERN, null);
        if (regExpr != null) {
            try {
                if (m_caseSensitive) {
                    m_pattern = Pattern.compile(regExpr);
                } else {
                    m_pattern = Pattern.compile(regExpr,
                            Pattern.CASE_INSENSITIVE);
                }
            } catch (PatternSyntaxException pse) {
                throw new InvalidSettingsException("Column value filter: "
                        + "NodeSettings object contains invalid reg expr.");
            }
        } else {
            m_pattern = null;
        }

        m_lowerBound = cfg.getDataCell(CFG_LOWERBOUND, null);
        m_upperBound = cfg.getDataCell(CFG_UPPERBOUND, null);

        if ((m_lowerBound == null) && (m_upperBound == null)
                && (m_pattern == null)) {
            throw new InvalidSettingsException("Column value filter: "
                    + "NodeSettings object contains no matching criteria");
        }

        if ((m_pattern != null)
                && ((m_lowerBound != null) || (m_upperBound != null))) {
            throw new InvalidSettingsException("Column value filter: "
                    + "Invalid NodeSettings object; "
                    + "can't match range and pattern.");
        }

    }

    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #saveSettings(de.unikn.knime.core.node.NodeSettings)
     */
    protected void saveSettings(final NodeSettings cfg) {
        cfg.addBoolean(CFG_INCLUDE, m_include);
        cfg.addInt(CFG_COLINDEX, m_colIndex);
        if (m_pattern == null) {
            cfg.addString(CFG_PATTERN, null);
        } else {
            cfg.addString(CFG_PATTERN, m_pattern.pattern());
        }
        cfg.addBoolean(CFG_CASESENSE, m_caseSensitive);
        cfg.addBoolean(CFG_STARTSWITH, m_startsWith);
        cfg.addDataCell(CFG_LOWERBOUND, m_lowerBound);
        cfg.addDataCell(CFG_UPPERBOUND, m_upperBound);
    }

    /**
     * the column value filter grabs the comparator from the table spec (if
     * available) and checks settings against the latest spec.
     * 
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {

        if ((inSpec == null) || (inSpec.getNumColumns() <= 0)) {
            m_dcComp = null;
            return null;
        }
        if (inSpec.getNumColumns() <= m_colIndex) {
            throw new InvalidSettingsException("Column value filter: Selected"
                    + " column index out of range.");
        }
        DataType colType = inSpec.getColumnSpec(m_colIndex).getType();
        if (m_lowerBound != null) {
            if (!colType.isASuperTypeOf(m_lowerBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified lower bound of range doesn't fit "
                        + "column type. (Col#:"
                        + m_colIndex
                        + ",ColType:"
                        + colType.getClass().getName().substring(
                                colType.getClass().getName().lastIndexOf('.'))
                        + ",RangeType:"
                        + m_lowerBound.getType().getClass().getName().
                                substring(
                                        m_lowerBound.getType().getClass().
                                                getName().lastIndexOf('.')));
            }
        }
        if (m_upperBound != null) {
            if (!colType.isASuperTypeOf(m_upperBound.getType())) {
                throw new InvalidSettingsException("Column value filter: "
                        + "Specified upper bound of range doesn't fit "
                        + "column type. (Col#:"
                        + m_colIndex
                        + ",ColType:"
                        + colType.getClass().getName().substring(
                                colType.getClass().getName().lastIndexOf('.'))
                        + ",RangeType:"
                        + m_upperBound.getType().getClass().getName().
                                substring(
                                        m_upperBound.getType().getClass().
                                                getName().lastIndexOf('.')));
            }
        }

        m_dcComp = colType.getComparator();
        // TODO: if range is specidifed: modify range of colSpec, if pattern
        // is set, filter possible values with pattern.
        return null;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String result = "ColVal-Filter: Col# " + m_colIndex;
        result += m_include ? ", include " : ", exclude ";
        if (testingStringPattern()) {
            result += " pattern matching '" + m_pattern.pattern() + "' (";
            result += m_caseSensitive ? "case, " : "nocase, ";
            result += m_startsWith ? "prefix)" : "entire)";
        }
        if (rangeSet()) {
            result += " values from '";
            result += (m_lowerBound == null) ? "<open>" : m_lowerBound.
                    toString();
            result += "' to '";
            result += (m_upperBound == null) ? "<open>" : m_upperBound.
                    toString();            result += "'";
            if (m_dcComp == null) {
                result += " NO COMPARATOR SET!!!";
            }
        }
        return result;
    }

}
