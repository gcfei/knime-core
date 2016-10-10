/*
 * ------------------------------------------------------------------------
y *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.time.node.convert.oldtonew;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The {@link NodeModel} implementation of the node which converts old to new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class OldToNewTimeNodeModel extends NodeModel {

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelBoolean m_autoType = createTypeModelBool();

    private final SettingsModelString m_typeSelect = createTypeSelectModel();

    private final SettingsModelBoolean m_addZone = createZoneModelBool();

    private final SettingsModelString m_timeZone = createTimeZoneSelectModel();

    private DateTimeTypes[] m_newTypes = null;

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", DateAndTimeValue.class);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createTypeModelBool() {
        return new SettingsModelBoolean("type_bool", true);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createTypeSelectModel() {
        final SettingsModelString settingsModelString =
            new SettingsModelString("type_select", DateTimeTypes.LOCAL_DATE_TIME.toString());
        settingsModelString.setEnabled(false);
        return settingsModelString;
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createZoneModelBool() {
        return new SettingsModelBoolean("zone_bool", false);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createTimeZoneSelectModel() {
        final SettingsModelString settingsModelString =
            new SettingsModelString("time_zone_select", ZoneId.systemDefault().getId());
        settingsModelString.setEnabled(false);
        return settingsModelString;
    }

    /** One in, one out. */
    OldToNewTimeNodeModel() {
        super(1, 1);
    }

    /**
     * @param inSpec Current input spec
     * @return The CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final DataRow row) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndexes =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();

        final DataColumnSpec[] newColumnSpecs = getNewIncludedColumnSpecs(inSpec, row);
        int i = 0;
        for (String includedCol : includeList) {
            ConvertTimeCellFactory cellFac = new ConvertTimeCellFactory(newColumnSpecs[i], i, includeIndexes[i++]);
            rearranger.replace(cellFac, includedCol);
        }
        return rearranger;
    }

    /**
     *
     * @param inSpec Current input spec
     * @param row First row of the table, if called by execute, or null, if called by configure
     * @return Column specs of the output (only of the included columns)
     */
    private DataColumnSpec[] getNewIncludedColumnSpecs(final DataTableSpec inSpec, final DataRow row) {
        final String[] includes = m_colSelect.applyTo(inSpec).getIncludes();
        m_newTypes = new DateTimeTypes[includes.length];
        final DataColumnSpec[] newSpec = new DataColumnSpec[includes.length];

        /*
         * if the types of the cells should determined automatically by the content of the first row
         */
        if (m_autoType.getBooleanValue()) {
            // row is null, if the method is called by the configure method
            if (row != null) {
                DataColumnSpecCreator dataColumnSpecCreator = null;
                for (int i = 0; i < includes.length; i++) {
                    if (!m_addZone.getBooleanValue()) {
                        final DataCell cell = row.getCell(inSpec.findColumnIndex(includes[i]));
                        if (cell.isMissing()) {
                            m_newTypes[i] = DateTimeTypes.LOCAL_DATE_TIME;
                            dataColumnSpecCreator =
                                new DataColumnSpecCreator(includes[i], DataType.getType(LocalDateTimeCell.class));
                        } else {
                            final DateAndTimeCell timeCell = (DateAndTimeCell)cell;
                            if (!timeCell.hasDate()) {
                                m_newTypes[i] = DateTimeTypes.LOCAL_TIME;
                                dataColumnSpecCreator =
                                    new DataColumnSpecCreator(includes[i], DataType.getType(LocalTimeCell.class));
                            } else {
                                if (!timeCell.hasTime()) {
                                    m_newTypes[i] = DateTimeTypes.LOCAL_DATE;
                                    dataColumnSpecCreator =
                                        new DataColumnSpecCreator(includes[i], DataType.getType(LocalDateCell.class));
                                } else {
                                    m_newTypes[i] = DateTimeTypes.LOCAL_DATE_TIME;
                                    dataColumnSpecCreator = new DataColumnSpecCreator(includes[i],
                                        DataType.getType(LocalDateTimeCell.class));
                                }
                            }
                        }
                    } else {
                        m_newTypes[i] = DateTimeTypes.ZONED_DATE_TIME;
                        dataColumnSpecCreator =
                            new DataColumnSpecCreator(includes[i], DataType.getType(ZonedDateTimeCell.class));
                    }
                    newSpec[i] = dataColumnSpecCreator.createSpec();
                }
                return newSpec;
                // row is not null, if the method is called by the execute method
            } else {
                DataColumnSpecCreator dataColumnSpecCreator;
                for (int i = 0; i < includes.length; i++) {
                    if (m_addZone.getBooleanValue()) {
                        dataColumnSpecCreator =
                            new DataColumnSpecCreator(includes[i], DataType.getType(ZonedDateTimeCell.class));
                    } else {

                        dataColumnSpecCreator =
                            new DataColumnSpecCreator(includes[i], DataType.getType(LocalDateTimeCell.class));
                    }
                    newSpec[i] = dataColumnSpecCreator.createSpec();
                }
                return null;
            }
            /*
             * if the type of the new cells is determined by the user itself
             */
        } else {
            DataType newDataType = null;
            DateTimeTypes type = null;
            final String typeString = m_typeSelect.getStringValue();
            if (typeString.equals(DateTimeTypes.LOCAL_DATE.toString())) {
                newDataType = DataType.getType(LocalDateCell.class);
                type = DateTimeTypes.LOCAL_DATE;
            } else if (typeString.equals(DateTimeTypes.LOCAL_TIME.toString())) {
                newDataType = DataType.getType(LocalTimeCell.class);
                type = DateTimeTypes.LOCAL_TIME;
            } else if (typeString.equals(DateTimeTypes.LOCAL_DATE_TIME.toString())) {
                newDataType = DataType.getType(LocalDateTimeCell.class);
                type = DateTimeTypes.LOCAL_DATE_TIME;
            } else if (typeString.equals(DateTimeTypes.ZONED_DATE_TIME.toString())) {
                newDataType = DataType.getType(ZonedDateTimeCell.class);
                type = DateTimeTypes.ZONED_DATE_TIME;
            }
            for (int i = 0; i < includes.length; i++) {
                final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includes[i], newDataType);
                newSpec[i] = dataColumnSpecCreator.createSpec();
                m_newTypes[i] = type;
            }
            return newSpec;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataColumnSpec[] newColumnSpecs = getNewIncludedColumnSpecs(inSpecs[0], null);
        if (newColumnSpecs != null) {
            // merge the outspecs (included and excluded)
            final int[] includeIndexes = Arrays.stream(m_colSelect.applyTo(inSpecs[0]).getIncludes())
                .mapToInt(s -> inSpecs[0].findColumnIndex(s)).toArray();
            final DataColumnSpec[] colSpecs = new DataColumnSpec[inSpecs[0].getNumColumns()];
            for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
                final int searchIdx = Arrays.binarySearch(includeIndexes, i);
                if (searchIdx < 0) {
                    colSpecs[i] = inSpecs[0].getColumnSpec(i);
                } else {
                    colSpecs[i] = newColumnSpecs[searchIdx];
                }
            }

            return new DataTableSpec[]{new DataTableSpec(colSpecs)};
        } else {
            return new DataTableSpec[]{null};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        if (inObjects[0].size() > 0) {
            final ColumnRearranger columnRearranger =
                createColumnRearranger(inObjects[0].getDataTableSpec(), inObjects[0].iterator().next());
            final BufferedDataTable out = exec.createColumnRearrangeTable(inObjects[0], columnRearranger, exec);
            return new BufferedDataTable[]{out};
        } else {
            return inObjects;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_autoType.saveSettingsTo(settings);
        m_typeSelect.saveSettingsTo(settings);
        m_addZone.saveSettingsTo(settings);
        m_timeZone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_autoType.validateSettings(settings);
        m_typeSelect.validateSettings(settings);
        m_addZone.validateSettings(settings);
        m_timeZone.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_autoType.loadSettingsFrom(settings);
        m_typeSelect.loadSettingsFrom(settings);
        m_addZone.loadSettingsFrom(settings);
        m_timeZone.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

    /**
     *
     *
     */
    final class ConvertTimeCellFactory extends SingleCellFactory {

        private final int m_typeIndex;

        private final int m_colIndex;

        /**
         * @param inSpec
         * @param typeIndex
         * @param colIndex
         */
        public ConvertTimeCellFactory(final DataColumnSpec inSpec, final int typeIndex, final int colIndex) {
            super(inSpec);
            m_typeIndex = typeIndex;
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final DateAndTimeCell timeCell = (DateAndTimeCell)cell;
            int millis = 0;
            if (timeCell.hasMillis()) {
                millis = timeCell.getMillis();
            }
            switch (m_newTypes[m_typeIndex]) {
                case LOCAL_DATE: {
                    try {
                        final LocalDate ld =
                            LocalDate.of(timeCell.getYear(), timeCell.getMonth() + 1, timeCell.getDayOfMonth());
                        return LocalDateCellFactory.create(ld);
                    } catch (Exception e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                case LOCAL_TIME: {
                    try {
                        final LocalTime lt = LocalTime.of(timeCell.getHourOfDay(), timeCell.getMinute(),
                            timeCell.getSecond(), (int)TimeUnit.MILLISECONDS.toNanos(millis));
                        return LocalTimeCellFactory.create(lt);
                    } catch (Exception e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                case LOCAL_DATE_TIME: {
                    try {
                        final LocalDate ld =
                            LocalDate.of(timeCell.getYear(), timeCell.getMonth() + 1, timeCell.getDayOfMonth());
                        final LocalTime lt = LocalTime.of(timeCell.getHourOfDay(), timeCell.getMinute(),
                            timeCell.getSecond(), (int)TimeUnit.MILLISECONDS.toNanos(millis));
                        return LocalDateTimeCellFactory.create(LocalDateTime.of(ld, lt));
                    } catch (Exception e) {
                        return new MissingCell(e.getMessage());
                    }
                }
                case ZONED_DATE_TIME: {
                    try {
                        final LocalDate ld =
                            LocalDate.of(timeCell.getYear(), timeCell.getMonth() + 1, timeCell.getDayOfMonth());
                        final LocalTime lt = LocalTime.of(timeCell.getHourOfDay(), timeCell.getMinute(),
                            timeCell.getSecond(), (int)TimeUnit.MILLISECONDS.toNanos(millis));
                        return ZonedDateTimeCellFactory
                            .create(ZonedDateTime.of(ld, lt, ZoneId.of(m_timeZone.getStringValue())));
                    } catch (Exception e) {
                        return new MissingCell(e.getMessage());
                    }
                }
            }
            return null;
        }
    }

}
