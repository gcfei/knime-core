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
 * History
 *   09.05.2005 (mb): created
 *   12.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import de.unikn.knime.core.node.DefaultNodeProgressMonitor;

/** Convenience Class that supports Execution of a Workflow stored within
 * a <code>WorkflowManager</code>.
 * Currently it simply starts a new Thread for every executable Node and
 * returns when the Workflow is done.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class WorkflowExecutor implements WorkflowListener {
    private final WorkflowManager m_flowMgr;
    
    /** Create executor class and register as listener for events.
     * 
     * @param mgr WorkflowManager to be executed
     */
    public WorkflowExecutor(final WorkflowManager mgr) {
        m_flowMgr = mgr;
        m_flowMgr.addListener(this);
    }

    /** Execute all nodes in workflow - return when all nodes
     * are executed (or at least Workflow claims to be done).
     */
    public void executeAll() {
        m_flowMgr.prepareForExecAllNodes();
        m_flowMgr.startExecution(true);
    }
    
    /** Execute all nodes in workflow leading to a certain node.
     * Return when all nodes are executed (or at least Workflow
     * claims to be done).
     * 
     * @param nodeID id of node to be executed.
     */
    public void executeUpToNode(final int nodeID) {
        m_flowMgr.prepareForExecUpToNode(nodeID);
        m_flowMgr.startExecution(true);
    }
    
    /**
     * Starts additional nodes when workflow has changed, stops execution when
     * workflow is done.
     * 
     * @param event the WorkflowEvent to be handled
     */
    public void workflowChanged(final WorkflowEvent event) {
        if (event instanceof WorkflowEvent.ExecPoolChanged) {
            NodeContainer nextNode = null;
            while ((nextNode = m_flowMgr.getNextExecutableNode()) != null) {
                nextNode.startExecution(new DefaultNodeProgressMonitor());
            }
        }
    }

}
