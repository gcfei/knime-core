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
 */
package org.knime.core.node.port.database;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Utility class to load additional drivers from jar and zip to the
 * <code>DriverManager</code>.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DatabaseDriverLoader {
    private static final String EXT_POINT_ID = "org.knime.core.JDBCDriver";

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DatabaseDriverLoader.class);

    /** Map from driver file to driver class. */
    private static final Map<String, File> DRIVERFILE_TO_DRIVERCLASS
        = new LinkedHashMap<String, File>();

    /**
     * Name of the standard JDBC-ODBC database driver,
     * <i>sun.jdbc.odbc.JdbcOdbcDriver</i> object. Loaded per default.
     */
    static final String JDBC_ODBC_DRIVER = "sun.jdbc.odbc.JdbcOdbcDriver";

    private static final Map<String, String> DRIVER_TO_URL
        = new LinkedHashMap<String, String>();

    /**
     * Allowed file extensions, jar and zip only.
     */
    public static final String[] EXTENSIONS = {".jar", ".zip"};

    private static final ClassLoader CLASS_LOADER = ClassLoader
            .getSystemClassLoader();

    private static final Map<String, DatabaseWrappedDriver> DRIVER_MAP
        = new HashMap<String, DatabaseWrappedDriver>();

    /**
     * Creates a mapping from JDBC driver class names to the corresponding JDBC connection strings.
     */
    private static void createDriverProtocolMapping() {
        DRIVER_TO_URL.put(JDBC_ODBC_DRIVER, "jdbc:odbc:");
        DRIVER_TO_URL.put("com.ibm.db2.jcc.DB2Driver", "jdbc:db2:");
        DRIVER_TO_URL.put("org.firebirdsql.jdbc.FBDriver", "jdbc:firebirdsql:");
        DRIVER_TO_URL.put("com.mysql.jdbc.Driver", "jdbc:mysql:");
        DRIVER_TO_URL.put("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:");
        DRIVER_TO_URL.put("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:");
        DRIVER_TO_URL.put("org.postgresql.Driver", "jdbc:postgresql:");
        DRIVER_TO_URL.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver:");
        DRIVER_TO_URL.put("com.microsoft.jdbc.sqlserver.SQLServerDriver", "jdbc:microsoft:sqlserver:");
        DRIVER_TO_URL.put("org.apache.derby.jdbc.ClientDriver", "jdbc:derby:");
        DRIVER_TO_URL.put("jdbc.FrontBase.FBJDriver", "jdbc:FrontBase:");
        DRIVER_TO_URL.put("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql:");
        DRIVER_TO_URL.put("com.ingres.jdbc.IngresDriver", "jdbc:ingres:");
        DRIVER_TO_URL.put("com.openbase.jdbc.ObDriver", "jdbc:openbase:");
        DRIVER_TO_URL.put("net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds:sybase:");
        DRIVER_TO_URL.put("com.sybase.jdbc3.jdbc.SybDriver", "jdbc:sybase:Tds:");
        DRIVER_TO_URL.put("org.sqlite.JDBC", "jdbc:sqlite:");
    }

    static {
        // make sure that all static variables are initialized before (i.e. above) this static block
        createDriverProtocolMapping();
        registerODBCBridge();
        loadDriversFromExtensionPoint();
    }

    /**
     * Registers the native JDBC-ODBC bridge.
     */
    private static void registerODBCBridge() {
        try {
            // Bug 3821: prevent loading sun.jdbc.odbc.JdbcOdbcDriver driver on Linux (with missing libodbc.so library)
            if (Platform.OS_LINUX.equals(Platform.getOS())) {
                try {
                    System.loadLibrary("libJdbcOdbc.so");
                } catch (Error e) {
                    LOGGER.info("Could not load \"libJdbcOdbc.so\" library which is known to be a problem under Linux"
                        + " when using the \"" + JDBC_ODBC_DRIVER + "\"; that is, the driver is not loaded.");
                    // don't load driver
                    return;
                }
            }
            Class<?> driverClass = Class.forName(JDBC_ODBC_DRIVER);
            DatabaseWrappedDriver d = new DatabaseWrappedDriver((Driver)driverClass.newInstance());
            // DriverManager.registerDriver(d);
            DRIVER_MAP.put(d.toString(), d);
        } catch (Throwable t) {
            LOGGER.warn("Could not load driver class \"" + JDBC_ODBC_DRIVER + "\"");
        }
    }

    /** Hide (empty) constructor. */
    private DatabaseDriverLoader() {
        // empty default constructor
    }

    /**
     * Registers given <code>Driver</code> at the <code>DriverManager</code>.
     * @param driver to register
     * @return SQL Driver
     * @throws InvalidSettingsException if the database drivers could not
     *             registered
     */
    public static Driver registerDriver(final String driver)
            throws InvalidSettingsException {
        try {
            Driver wrappedDriver =
                DatabaseDriverLoader.getWrappedDriver(driver);
            DriverManager.registerDriver(wrappedDriver);
            return wrappedDriver;
        } catch (Throwable t) {
            throw new InvalidSettingsException("Could not register database"
                  + " driver \"" + driver + "\", reason: " + t.getMessage(), t);
        }
    }

    /**
     * Loads <code>Driver</code> from the given file.
     *
     * @param file Load driver from.
     * @throws IOException {@link IOException}
     */
    public static void loadDriver(final File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File \"" + file + "\" does not exist.");
        }
        if (file.isDirectory()) {
            throw new IOException("File \"" + file
                    + "\" can't be a directory.");
        }
        final String fileName = file.getAbsolutePath();
        if (!fileName.endsWith(".jar") && !fileName.endsWith(".zip")) {
            throw new IOException("Unsupported file \"" + file + "\","
                + " only zip and jar files are allowed.");
        }
        if (DRIVERFILE_TO_DRIVERCLASS.containsValue(file)) {
            return;
        }
        readZip(file, new JarFile(file));
    }

    private static void readZip(final File file, final ZipFile zipFile)
            throws MalformedURLException {
        final ClassLoader cl = new URLClassLoader(
                new URL[]{file.toURI().toURL()}, CLASS_LOADER);
        for (Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            zipEntries.hasMoreElements();) {
            ZipEntry e = zipEntries.nextElement();
            if (e == null) {
                continue;
            }
            String name = e.getName();
            if (name.endsWith(".class")) {
                   try {
                    Class<?> c = loadClass(cl, name);
                    if (Driver.class.isAssignableFrom(c)) {
                        DatabaseWrappedDriver d = new DatabaseWrappedDriver(
                                (Driver)c.newInstance());
                        String driverName = d.toString();
                        DRIVER_MAP.put(driverName, d);
                        DRIVERFILE_TO_DRIVERCLASS.put(driverName, file);
                    }
                } catch (Throwable t) {
                    // ignored
                }
            }
        }
    }

    /**
     * @return A set if loaded driver names.
     */
    public static Set<String> getLoadedDriver() {
        return DRIVER_MAP.keySet();
    }

    /**
     * Get driver for name. If no corresponding driver can be found, it looks up
     * the current class path in order to try to instantiate this class.
     * @param driverName The driver name.
     * @return The <code>WrappedDriver</code> for the given driver name.
     */
    private static DatabaseWrappedDriver getWrappedDriver(
            final String driverName) throws Exception {
        DatabaseWrappedDriver wdriver = DRIVER_MAP.get(driverName);
        if (wdriver != null) {
            return wdriver;
        }
        // otherwise try to load new driver from registered classes
        Class<?> c = Class.forName(driverName, true,
                ClassLoader.getSystemClassLoader());
        DatabaseWrappedDriver d = new DatabaseWrappedDriver(
                (Driver) c.newInstance());
        DRIVER_MAP.put(driverName, d);
        return d;
    }

    private static Class<?> loadClass(final ClassLoader cl, final String name)
            throws ClassNotFoundException {
        String newName = name.substring(0, name.indexOf(".class"));
        String className = newName.replace('/', '.');
        return cl.loadClass(className);
    }

    /**
     * Returns a URL protocol for a given <code>Driver</code> extended by
     * an default host, port, database name String. If no protocol URL has been
     * defined the default String staring with protocol is return.
     * @param driver the driver to match URL protocol
     * @return an String containing protocol, port, host, and database name
     *      place holder
     */
    public static String getURLForDriver(final String driver) {
        String url = DRIVER_TO_URL.get(driver);
        if (url == null) {
            return "<protocol>://<host>:<port>/<database_name>";
        }
        return url + "//<host>:<port>/<database_name>";
    }

    /**
     * Returns the absolute path for the driver class name from which it has
     * been loaded.
     * @param driverClass driver class name
     * @return driver file location
     */
    public static File getDriverFileForDriverClass(
            final String driverClass) {
        return DRIVERFILE_TO_DRIVERCLASS.get(driverClass);
    }


    /**
     * Loads all JDBC driver registered via the extension point.
     */
    private static void loadDriversFromExtensionPoint() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point id: " + EXT_POINT_ID);

        }
        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement e : elements) {
                String path = e.getAttribute("jarFile");
                String bundleId = e.getDeclaringExtension().getNamespaceIdentifier();

                URL jdbcUrl = Platform.getBundle(bundleId).getEntry(path);
                try {
                    loadDriver(new File(FileLocator.toFileURL(jdbcUrl).getPath()));
                } catch (IOException ex) {
                    LOGGER.error("Could not load JDBC driver '" + path + "': " + ex.getMessage(), ex);
                }
            }
        }
    }
}
