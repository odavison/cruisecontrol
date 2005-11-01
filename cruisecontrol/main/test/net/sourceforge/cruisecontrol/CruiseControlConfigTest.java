/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestNestedPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestSelfConfiguringPlugin;
import net.sourceforge.cruisecontrol.listeners.ListenerTestOtherNestedPlugin;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.net.URL;

public class CruiseControlConfigTest extends TestCase {

    private CruiseControlConfig config;
    private File configFile;
    private File tempDirectory;
    private File propertiesFile;
    private Level defaultThreshold;

    private static final int ONE_SECOND = 1000;

    protected void setUp() throws Exception {
        // Turn off logging
        BasicConfigurator.configure();
        defaultThreshold = Logger.getLogger(this.getClass()).getLoggerRepository().getThreshold();
        Logger.getLogger(this.getClass()).getLoggerRepository().setThreshold(Level.ERROR);
      
        URL url;
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/test.properties");
        propertiesFile = new File(url.getPath());

        // Set up a CruiseControl config file for testing
        url = this.getClass().getClassLoader().getResource("net/sourceforge/cruisecontrol/testconfig.xml");
        configFile = new File(url.getPath());
        tempDirectory = configFile.getParentFile();

        Element rootElement = Util.loadConfigFile(configFile);
        Properties globalProperties = new Properties();
        globalProperties.put("test.properties.dir", propertiesFile.getParentFile().getAbsolutePath());
        config = new CruiseControlConfig(globalProperties);
        config.configure(rootElement);
    }

    protected void tearDown() {
        Logger.getLogger(this.getClass()).getLoggerRepository().setThreshold(defaultThreshold);
      
        // The directory "foo" in the system's temporary file location
        // is created by CruiseControl when using the config file below.
        // Specifically because of the line:
        //     <log dir='" + tempDirPath + "/foo' encoding='utf-8' >
        File fooDirectory = new File(tempDirectory, "foo");
        fooDirectory.delete();
    }

    public void testGetProjectNames() {
        assertEquals(16, config.getProjectNames().size());
    }

    public void testGlobalProperty() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("simpleprops");
        Properties props = projConfig.getProperties();
        assertEquals(5, props.size()); // 4 in file, one global passed to consutructor
        assertEquals("works!", props.getProperty("global"));
    }

    public void testProjectNameProperty() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("project1");
        Properties props = projConfig.getProperties();
        assertEquals(4, props.size());
        assertEquals("project1", props.getProperty("project.name"));
    }    

    public void testProjectNameInGlobalProperty() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("project1");
        Properties props = projConfig.getProperties();
        assertEquals(4, props.size());
        assertEquals("works!", props.getProperty("global"));
        assertEquals("project1", props.getProperty("project.name"));
        assertEquals("project=project1", props.getProperty("project.global"));
    }    

    public void testSimpleProperty() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("simpleprops");
        Properties props = projConfig.getProperties();
        assertEquals(5, props.size());
        assertEquals("success!", props.getProperty("simple"));
    }

    public void testMultipleProperties() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("multiprops");
        Properties props = projConfig.getProperties();
        assertEquals(8, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("one.two$three", props.getProperty("multi"));
    }

    public void testNestedProperties() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("nestedprops");
        Properties props = projConfig.getProperties();
        assertEquals(10, props.size());
        assertEquals("one", props.getProperty("first"));
        assertEquals("two", props.getProperty("second"));
        assertEquals("three", props.getProperty("third"));
        assertEquals("almost", props.getProperty("one.two.three"));
        assertEquals("threeLevelsDeep", props.getProperty("almost"));
        assertEquals("threeLevelsDeep", props.getProperty("nested"));
    }

    public void testPropertyEclipsing() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("eclipseprop");
        Properties props = projConfig.getProperties();
        assertEquals(4, props.size());
        assertEquals("eclipsed", props.getProperty("global"));
    }
    
    public void testLoadPropertiesFromFile() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("propsfromfile");
        Properties props = projConfig.getProperties();
        assertEquals(9, props.size());
        assertEquals("/home/cruise", props.getProperty("dir1"));
        assertEquals("/home/cruise/logs", props.getProperty("dir2"));
        assertEquals("temp", props.getProperty("tempdir"));
        assertEquals("/home/cruise/logs/temp", props.getProperty("multi"));
    }

    // test that we are capable of resolving properties in all property attributes
    public void testPropertiesInProperties() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("propsinpropsdef");
        Properties props = projConfig.getProperties();
        // these ones where defined normally, shouldn't be any problem
        assertEquals("true", props.getProperty("env.toupper"));
        assertEquals("env", props.getProperty("env.prefix"));

        assertEquals("Resolving property file name attribute worked",
                     "/home/cruise", props.getProperty("dir1"));
        assertEquals("Resolving property name attribute worked",
                      "test1", props.getProperty("test1"));
        int nbEnvPropertiesFound = 0;
        for (Enumeration propertyNames = props.propertyNames(); propertyNames.hasMoreElements(); ) {
           String name = (String) propertyNames.nextElement();
           if (name.startsWith("env.")) {
             nbEnvPropertiesFound++;
           }
        }
        assertTrue("Resolving environment prefix attribute worked",
                       nbEnvPropertiesFound > 0);
        assertNotNull("Resolving environment prefix and touuper attributes worked",
                       props.getProperty("env.PATH"));
    }

    // TODO backport
    /*
    public void testMissingProperty() {
        // there's in fact little need to check for both cases.
        // This will be hardcoded at some point and the default case.
        // Feel free to scrap the first if when checking it in.
        if (ProjectXMLHelper.FAIL_UPON_MISSING_PROPERTY) {
            try {
                createProjectXMLHelper("missingprop");
                fail("A missing property should cause an exception!");
            } catch (CruiseControlException expected) {
            }
        } else {
            try {
                createProjectXMLHelper("missingprop");
            } catch (CruiseControlException unexpected) {
                fail(unexpected.getMessage());
            }
        }
    }
    */

    // TODO this a test of the PluginHelper
    public void testGetPluginConfigNoOverride() throws Exception {
        ProjectConfig projConfig = config.getConfig("project1");
        PluginRegistry registry = config.getProjectPlugins("project1");

        assertEquals(ListenerTestNestedPlugin.class, registry.getPluginClass("testnested"));

        final ProjectXMLHelper helper
            = new ProjectXMLHelper(projConfig.getProperties(), registry);

        PluginXMLHelper pluginHelper = new PluginXMLHelper(helper);
        Object plugin;

        plugin = helper.getConfiguredPlugin(pluginHelper, "testnested");
        assertEquals(ListenerTestNestedPlugin.class, plugin.getClass());
        ListenerTestNestedPlugin plug1 = (ListenerTestNestedPlugin) plugin;
        assertEquals("default", plug1.getString());
        assertEquals("otherdefault", plug1.getOtherString());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testselfconfiguring");
        assertEquals(null, plugin);

        plugin = helper.getConfiguredPlugin(pluginHelper, "testlistener");
        assertEquals(null, plugin);
    }

    // TODO this a test of the PluginHelper
    public void testGetPluginConfig() throws Exception {
        ProjectConfig projConfig = config.getConfig("project4");
        PluginRegistry registry = config.getProjectPlugins("project4");

        final ProjectXMLHelper helper
            = new ProjectXMLHelper(projConfig.getProperties(), registry);

        PluginXMLHelper pluginHelper = new PluginXMLHelper(helper);
        Object plugin;

        plugin = helper.getConfiguredPlugin(pluginHelper, "testnested");
        assertEquals(ListenerTestNestedPlugin.class, plugin.getClass());
        ListenerTestNestedPlugin plug1 = (ListenerTestNestedPlugin) plugin;
        assertEquals("overriden", plug1.getString());
        // not overriden
        assertEquals("otherdefault", plug1.getOtherString());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testselfconfiguring");
        assertEquals(ListenerTestSelfConfiguringPlugin.class, plugin.getClass());
        ListenerTestSelfConfiguringPlugin plug2 = (ListenerTestSelfConfiguringPlugin) plugin;
        assertEquals(null, plug2.getString());
        assertEquals(null, plug2.getNested());

        plugin = helper.getConfiguredPlugin(pluginHelper, "testlistener");
        assertEquals(ListenerTestPlugin.class, plugin.getClass());
        ListenerTestPlugin plug3 = (ListenerTestPlugin) plugin;
        assertEquals("project4-0", plug3.getString());
    }

    public void testPluginConfiguration() throws Exception {
        ProjectConfig projConfig = config.getConfig("project4");
        PluginRegistry plugins = config.getProjectPlugins("project4");

        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestNestedPlugin.class, plugins.getPluginClass("testnested"));
        assertEquals(ListenerTestSelfConfiguringPlugin.class, plugins.getPluginClass("testselfconfiguring"));

        List listeners = projConfig.getListeners();
        assertEquals(3, listeners.size());

        Listener listener0 = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener0.getClass());
        ListenerTestPlugin testListener0 = (ListenerTestPlugin) listener0;
        assertEquals("project4-0", testListener0.getString());

        Listener listener1 = (Listener) listeners.get(1);
        assertEquals(ListenerTestPlugin.class, listener1.getClass());
        ListenerTestPlugin testListener1 = (ListenerTestPlugin) listener1;
        assertEquals("listener1", testListener1.getString());
        assertEquals("wrapper1", testListener1.getStringWrapper().getString());

        Listener listener2 = (Listener) listeners.get(2);
        assertEquals(ListenerTestPlugin.class, listener2.getClass());
        ListenerTestPlugin testListener2 = (ListenerTestPlugin) listener2;
        assertEquals("listener2", testListener2.getString());
        // note this is in fact undefined behavior!! Because we added twice the stringwrapper
        // (first for the child, then for the parent).
        // this could probably fail depending on a different platform, except if Element.setContent()
        // specifies the order in which children are kept within the element.
        final String wrapper = testListener2.getStringWrapper().getString();
        assertTrue("wrapper2-works!", "wrapper2-works!".equals(wrapper)
                                      || "wrapper1".equals(wrapper));
    }

    public void testPluginConfigurationClassOverride() throws Exception {
        ProjectConfig projConfig = config.getConfig("project5");
        PluginRegistry plugins = config.getProjectPlugins("project5");

        assertEquals(ListenerTestPlugin.class, plugins.getPluginClass("testlistener"));
        assertEquals(ListenerTestOtherNestedPlugin.class, plugins.getPluginClass("testnested"));

        List listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());

        Listener listener0 = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener0.getClass());
        ListenerTestPlugin testListener0 = (ListenerTestPlugin) listener0;
        assertEquals("default", testListener0.getString());
        ListenerTestNestedPlugin nested = testListener0.getNested();
        assertTrue(nested instanceof ListenerTestOtherNestedPlugin);
        assertEquals("notshadowing", ((ListenerTestOtherNestedPlugin) nested).getString());
        assertEquals(null, ((ListenerTestOtherNestedPlugin) nested).getOtherString());
        assertEquals("otherother", ((ListenerTestOtherNestedPlugin) nested).getOtherOtherString());
    }

    // TODO DateFormat management was moved to Project.init()
    /*
    public void testDateFormat() throws Exception {
        final String originalFormat = DateFormatFactory.getFormat();
        createProjectXMLHelper("dateformatfromproperty");
        final String formatFromProperty = DateFormatFactory.getFormat();
        DateFormatFactory.setFormat(DateFormatFactory.DEFAULT_FORMAT);

        assertEquals(DateFormatFactory.DEFAULT_FORMAT, originalFormat);
        assertEquals("MM/dd/yyyy HH:mm:ss a", formatFromProperty);
        assertFalse(originalFormat.equals(formatFromProperty));
    }

    public void testPreconfigureDateFormat() throws Exception {
        final String originalFormat = DateFormatFactory.getFormat();
        createProjectXMLHelper("dateformatpreconfigured");
        final String formatFromProperty = DateFormatFactory.getFormat();
        DateFormatFactory.setFormat(DateFormatFactory.DEFAULT_FORMAT);

        assertEquals(DateFormatFactory.DEFAULT_FORMAT, originalFormat);
        assertEquals("MM/dd/yyyy HH:mm:ss a", formatFromProperty);
        assertFalse(originalFormat.equals(formatFromProperty));
    }
    */

    public void testGetBootstrappers() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("project1");

        List bootstrappers = projConfig.getBootstrappers();
        assertEquals(0, bootstrappers.size());

        projConfig = config.getConfig("project2");
        bootstrappers = projConfig.getBootstrappers();
        assertEquals(1, bootstrappers.size());
    }

    public void testGetSchedule() throws CruiseControlException {
        ProjectConfig projConfig;
        // TODO
        /*
        projConfig = config.getConfig("project1");
        try {
            projConfig.getSchedule();
            fail("schedule should be a required element");
        } catch (CruiseControlException expected) {
        }
        */

        projConfig = config.getConfig("project2");
        Schedule schedule = projConfig.getSchedule();
        assertEquals(20 * ONE_SECOND, schedule.getInterval());
    }

    public void testGetModificationSet() throws CruiseControlException {
        ProjectConfig projConfig;
        // TODO
        /*
        projConfig = config.getConfig("project1");
        try {
            projConfig.getModificationSet();
            fail("modificationset should be a required element");
        } catch (CruiseControlException expected) {
        }
        */

        projConfig = config.getConfig("project2");
        ModificationSet modSet = projConfig.getModificationSet();
        assertEquals(10 * ONE_SECOND, modSet.getQuietPeriod());
    }

    public void testGetLabelIncrementer() throws CruiseControlException {
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", CruiseControlConfig.LABEL_INCREMENTER);
        pluginElement.setAttribute("classname", DefaultLabelIncrementer.class.getName());
        PluginRegistry.registerToRoot(pluginElement);
        ProjectConfig projConfig = config.getConfig("project2");
        DefaultLabelIncrementer incrementer = (DefaultLabelIncrementer) projConfig.getLabelIncrementer();
        assertTrue(incrementer.isValidLabel("build#9"));

        projConfig = config.getConfig("project1");
        incrementer = (DefaultLabelIncrementer) projConfig.getLabelIncrementer();
        assertFalse(incrementer.isValidLabel("build#9"));
    }

    public void testGetLog() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("project1");
        assertEquals("logs" + File.separatorChar + "project1", projConfig.getLog().getLogDir());

        projConfig = config.getConfig("project2");
        assertEquals(tempDirectory.getAbsolutePath() + "/foo", projConfig.getLog().getLogDir());

        projConfig = config.getConfig("project3");
        assertEquals("logs" + File.separatorChar + "project3", projConfig.getLog().getLogDir());

        projConfig = config.getConfig("project3bis");
        assertEquals("logs/project3bis", projConfig.getLog().getLogDir());
        assertNull(projConfig.getLog().getLogXmlEncoding());

        projConfig = config.getConfig("project2");
        assertEquals("utf-8", projConfig.getLog().getLogXmlEncoding());
    }

    public void testPreconfigureLog() throws Exception {
        ProjectConfig projConfig = config.getConfig("logpreconfigured");

        final Log log = projConfig.getLog();
        assertEquals("mylogs/logpreconfigured", log.getLogDir());
        assertEquals("utf128", log.getLogXmlEncoding());
        assertEquals("logpreconfigured", log.getProjectName());

        BuildLogger[] loggers = log.getLoggers();
        assertEquals(2, loggers.length);
    }

    public void testGetListeners() throws CruiseControlException {
        ProjectConfig projConfig = config.getConfig("project1");
        List listeners = projConfig.getListeners();
        assertEquals(0, listeners.size());

        projConfig = config.getConfig("project2");
        listeners = projConfig.getListeners();
        assertEquals(1, listeners.size());
    }
}
