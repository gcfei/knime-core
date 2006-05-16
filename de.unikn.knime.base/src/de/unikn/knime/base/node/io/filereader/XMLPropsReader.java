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
 *   28.01.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.IllegalCharsetNameException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.xml.XMLProperties;

/**
 * Adds support for the old XML property files. It will read the specified XML
 * file and provides the values at its API.
 * 
 * @author ohl, University of Konstanz
 */
public class XMLPropsReader extends XMLProperties {

        /*
         * Store the location of the xml file we are looking at.
         */
        private final URL m_xmlFileURL;

        /*
         * The DTD to parse file with using the specification.
         */
        private static final String DTD = 
            XMLPropsReader.class.getName().replace('.', '/') + ".dtd";

        /**
         * Initializes a new specification object for a <code>FileTable</code>
         * using the XML <code>URL</code> which contains a link to the source
         * file as well as other properties how to parse the input data file.
         * 
         * @param xmlURL Specifies <code>URL</code> where to find the XML
         *            file.
         * 
         * @throws IllegalArgumentException If XML <code>URL</code> is
         *             <code>null</code>.
         * @throws IOException If XML <code>URL</code> is not valid.
         * @throws SAXException parser exception.
         * @throws ParserConfigurationException A parser configuration
         *             exception.
         */
        public XMLPropsReader(final URL xmlURL) throws IOException, 
                SAXException, ParserConfigurationException {
            // call super class to parse xml file with given DTD
            super(xmlURL, resolveDTD());
            m_xmlFileURL = xmlURL;
        }
        
        private static URL resolveDTD() {
            ClassLoader classLoader = XMLPropsReader.class.getClassLoader();
            return classLoader.getResource(DTD);
        }

        /* --- access functions --- */

        /**
         * Returns <code>FileTable</code> id.
         * 
         * @return The id for this table.
         */
        public String getId() {
            // return table id
            return getAttributeValue("FileTableSpec", "id", true);
        }

        /**
         * Get the missing value designator for the specified column.
         * 
         * @param index The index of the column to get the missing value
         *            representation for.
         * @return The value of the missing attr of the specified column
         */
        public String getColumnMissing(final int index) {
            // check index
            if (index < 0 || index >= getNumColumns()) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + ".");
            }
            // get node for column header
            Node node = getNodeElement("column_header", true);
            // get node's child list
            NodeList nodeList = node.getChildNodes();
            // and the node at index
            Node indexNode = nodeList.item(index);
            // check node
            if (indexNode == null) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + " <> " + nodeList.getLength() + ".");
            }
            // finally return DataCell with value of attribute "name"
            return getAttributeValue(indexNode, "missing");
        }

        /**
         * Returns number of columns as read from the xml file.
         * 
         * @return number of columns specified in the xml file.
         * @throws NumberFormatException if value is not parsable as integer
         *             number.
         */
        public int getNumColumns() {
            // this function reads it from the tree build by the XML parser.
            // get attribute value
            final String value = getAttributeValue("column_header", "number", 
                    true);
            // parse value as integer number
            return Integer.parseInt(value);
        }

        /**
         * Returns <code>true</code> if column header is specified in the data
         * file and must be read from data input file, otherwise
         * <code>false</code>.
         * 
         * @return <code>true</code> if column header must be read from input
         *         file, otherwise <code>false</code>.
         * 
         * @throws IllegalCharsetNameException if attribute is not either of
         *             value <i>true </i> or <i>false </i>.
         */
        public boolean isColumnHeaderSpecified() {
            // reads it from the tree build by the XML parser
            
            // get column header flag
            String value = getAttributeValue("column_header", "specified", 
                    true);
            // if value is "true", return true
            if (value.equals("true")) {
                return true;
            }
            // if value is "false", return false
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalCharsetNameException("Illegal value at element "
                    + "'column_header' with attribute 'specified': " 
                    + value + ".");
        }

        /**
         * Returns column name at index.
         * 
         * @param index in column header.
         * @return column name at index.
         * 
         * @throws IndexOutOfBoundsException if index out of range or number of
         *             columns not specified in xml file.
         */
        public DataCell getColumnName(final int index) {
            // reads it from the tree the XML parser built.
            
            // check index
            if (index < 0 || index >= getNumColumns()) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + ".");
            }
            // get node for column header
            Node node = getNodeElement("column_header", true);
            // get node's child list
            NodeList nodeList = node.getChildNodes();
            // and the node at index
            Node indexNode = nodeList.item(index);
            // check node
            if (indexNode == null) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + " <> " + nodeList.getLength() + ".");
            }
            // finally return DataCell with value of attribute "name"
            return new StringCell(getAttributeValue(indexNode, "name"));
        }


        /**
         * Returns column type as <code>DataType</code> at index.
         * 
         * @param index in column header.
         * @return column type as <code>DataType</code> at index.
         * @throws IndexOutOfBoundsException if index out of range.
         * @throws IllegalCharsetNameException if attribute value matches not
         *             one of the type: <i>String </i>, <i>Integer </i>, or
         *             <i>Double </i>.
         */
        public DataType getColumnType(final int index) {
            // this function reads it from the tree build by the XML parser.

            // check index
            if (index < 0 || index >= getNumColumns()) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + ".");
            }
            // get column header node
            Node node = getNodeElement("column_header", true);
            // get node's child list
            NodeList nodeList = node.getChildNodes();
            // and child not at index
            Node indexNode = nodeList.item(index);
            // check child node
            if (indexNode == null) {
                throw new IndexOutOfBoundsException("Index out of range: " 
                        + index + " <> " + nodeList.getLength() + ".");
            }
            // get attribute's value
            String attNodeValue = getAttributeValue(indexNode, "type");
            // if "String"
            if (attNodeValue.equals("String")) {
                return StringCell.TYPE;
            }
            // if "Integer"
            if (attNodeValue.equals("Integer")) {
                return IntCell.TYPE;
            }
            // if "Double"
            if (attNodeValue.equals("Double")) {
                return DoubleCell.TYPE;
            }
            // otherwise throw exception
            throw new IllegalCharsetNameException("Illegal column type at "
                    + "element 'column_header' with attribute 'type': " 
                    + attNodeValue + ".");
        } // getColumnType(int)

        /**
         * Get the <code>URL</code> of the XML file this spec was build from.
         * 
         * @return The <code>URL</code> of the XML file.
         */
        public URL getXMLFileURL() {
            return m_xmlFileURL;
        }

        /**
         * Returns the locator of the input data file as specified in the XML
         * file. It will not try to create an URL from it (in case it is
         * malformed). It will return a (not null) string of the form (parts in
         * brackets are optional): "['protocol':]['host'//][:'port]filename"
         * 
         * @return A non-null string containing the data file URL specifier from
         *         the XML file.
         */
        public String getDataFileURLspec() {

            StringBuffer dataURLspec = new StringBuffer("");

            // see if the protocol was specified
            Node protNode = getNodeElement("protocol", false);
            // check node
            if (protNode != null) {
                // get protocol attribute value
                dataURLspec.append(getAttributeValue(protNode, "value"));
                dataURLspec.append(":");
            }
            // add the host - if specified
            Node hostNode = getNodeElement("host", false);
            // check node
            if (hostNode != null) {
                // get host attribute value
                dataURLspec.append("//");
                getAttributeValue(hostNode, "value");
            }
            // see if we must add a port number
            Node portNode = getNodeElement("port", false);
            // check node
            if (portNode != null) {
                // get port attribute value (we don't enforce integer values...)
                dataURLspec.append(":");
                dataURLspec.append(getAttributeValue(portNode, "value"));
            }

            // add the (required) filename
            dataURLspec.append(getAttributeValue("url", "file_name", true));

            return dataURLspec.toString();
        }

        /**
         * Returns an <code>URL</code> of the input data file initialized with
         * file name, protocol, host, and port as specified in the xml file. If
         * a relative URL was specified it is being relativized against the URL
         * of the XML file.
         * 
         * @return input file <code>URL</code> or <code>null</code> if not
         *         available.
         * @throws MalformedURLException if <code>URL</code> cannt be
         *             initialized.
         */
        public URL getDataFileURL() throws MalformedURLException {
            URL dataURL;
            try {

                dataURL = new URL(m_xmlFileURL, getDataFileURLspec());
            } catch (MalformedURLException malURL) {
                String msg = "XML file: Couldn't create URL for data file '"
                        + getDataFileURLspec() + "' in the context of '"
                        + m_xmlFileURL.toString() + "'.";
                throw new MalformedURLException(msg);
            }

            return dataURL;
        }

        /**
         * Returns column delimiter string.
         * 
         * @return Column delimiter string or <code>null</code> if not
         *         available.
         */
        public String getColumnDelimiter() {
            return getAttributeValue("column_delimiter", "value", true);
        }

        /**
         * Returns <code>true</code> if row header is specified in the data
         * file and must be read from data input file, otherwise
         * <code>false</code>.
         * 
         * @return <code>true</code> if row header must be read from input
         *         file, otherwise <code>false</code>.
         * @throws IllegalCharsetNameException if attribute is not either of
         *             value <i>true </i> or <i>false </i>.
         */
        public boolean isRowHeaderSpecified() {
            // get row header flag
            String value = getAttributeValue("row_header", "specified", true);
            // if value is "true", return true
            if (value.equals("true")) {
                return true;
            }
            // if value is "false", return false
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalCharsetNameException("Illegal value at element "
                    + "'row_header' with attribute 'specified': " 
                    + value + ".");
        }

        /**
         * Returns the prefix for row keys.
         * 
         * @return row key prefix or <code>null</code> if not available.
         */
        public String getRowPrefix() {
            return getAttributeValue("row_header", "prefix", false);
        }

        /**
         * Returns row delimiter string.
         * 
         * @return row delimiter string or <code>null</code> if not available.
         */
        public String getRowDelimiter() {
            return getAttributeValue("row_delimiter", "value", false);
        }

        /**
         * Returns line comment string.
         * 
         * @return The line comment string or <code>null</code> if not
         *         available.
         */
        public String getLineComment() {
            // get assigned
            Node node = getNodeElement("line_comment", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "value");
        }

        /**
         * Returns the string for the left block comment.
         * 
         * @return String for left block comment.
         */
        public String getBlockCommentLeft() {
            // get assigned
            Node node = getNodeElement("block_comment", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "left");
        }

        /**
         * Returns the string for the right block comment.
         * 
         * @return String for right block comment.
         */
        public String getBlockCommentRight() {
            // get assigned
            Node node = getNodeElement("block_comment", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "right");
        }

        /**
         * Returns the string for the left quote.
         * 
         * @return String for left quote.
         */
        public String getQuoteLeft() {
            // get assigned
            Node node = getNodeElement("quote", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "left");
        }

        /**
         * Returns the string for the right quote.
         * 
         * @return String for right quote.
         */
        public String getQuoteRight() {
            // get assigned
            Node node = getNodeElement("quote", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "right");
        }

        /**
         * Returns escape quote string.
         * 
         * @return The escape quote string.
         */
        public String getQuoteEscape() {
            // get assigned
            Node node = getNodeElement("escape", false);
            // check node
            if (node == null) {
                return null;
            }
            // otherwise try to get attribute value
            return getAttributeValue(node, "value");
        }


}
