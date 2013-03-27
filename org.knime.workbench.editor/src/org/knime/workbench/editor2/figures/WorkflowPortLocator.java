/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowPortLocator extends PortLocator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowPortLocator.class);

    /**
     * @param type port type
     * @param portIndex port index
     * @param isInPort true if in port, false if out port
     * @param nrPorts total number of ports
     */
    public WorkflowPortLocator(final PortType type, final int portIndex,
            final boolean isInPort, final int nrPorts) {
        super(type, portIndex, isInPort, nrPorts, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final IFigure target) {
        Rectangle parent = target.getParent().getBounds().getCopy();

        int xPos = parent.x;
        if (isInPort()) {
            xPos += parent.width - AbstractPortFigure.WF_PORT_SIZE;
        }
        int yPos = (int)(parent.y + (((double)parent.height
                / (double) (getNrPorts() + 1)) * (getPortIndex() + 1)));
        yPos -= (AbstractPortFigure.WF_PORT_SIZE / 2);
        Rectangle portBounds = new Rectangle(new Point(xPos, yPos),
                new Dimension(
                        AbstractPortFigure.WF_PORT_SIZE,
                        AbstractPortFigure.WF_PORT_SIZE));

        LOGGER.debug("workflow port locator#relocate " + portBounds);
        target.setBounds(portBounds);
        target.repaint();
    }

}
