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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin;
import net.sourceforge.cruisecontrol.config.XMLConfigManager;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlControllerTest extends TestCase {

    private File dir = new File("target");
    private File configFile = new File(dir, "_tempConfigFile");
    private File configFile2 = new File(dir, "_tempConfigFile2");
    private CruiseControlController ccController;

    protected void setUp() throws CruiseControlException {
        dir.mkdirs();
        ccController = new CruiseControlController();
    }

    // makes sure the project is complete...
    static class MockConfigManager extends XMLConfigManager {
        public MockConfigManager(File file) throws CruiseControlException {
            super(file);
        }
        public ProjectConfig getConfig(String projectName) throws CruiseControlException {
            ProjectConfig config = super.getConfig(projectName);
            // the projects we work with do not have schedules in them...
            config.add(new Schedule());
            return config;
        }
    }

    // we need to lazy instanciate the config otherwise
    // the MockConfigManager tries to read the file when being built.
    class MockCruiseControlController extends CruiseControlController {
        /*
        ConfigManager configManager;

        protected ConfigManager getConfigManager() {
            if (configManager == null) {
                try {
                    configManager = new MockConfigManager(configFile);
                } catch (CruiseControlException e) {
                    throw new IllegalStateException("Failure to parsing configFile: " + configFile, e);
                }
            }
            return configManager;
        }
        */
    }

    public void tearDown() {
        if (configFile.exists()) {
            configFile.delete();
        }
        if (configFile2.exists()) {
            configFile2.delete();
        }
        ccController = null;
    }

    public void testSetNoFile() {
        try {
            ccController.setConfigFile(null);
            fail("Allowed to not set a config file");
        } catch (CruiseControlException expected) {
            assertEquals("No config file", expected.getMessage());
        }
        try {
            ccController.setConfigFile(configFile);
            fail("Config file must exist");
        } catch (CruiseControlException expected) {
            assertEquals("Config file not found: " + configFile.getAbsolutePath(), expected.getMessage());
        }
    }

    public void testLoadEmptyProjects() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertTrue(ccController.getProjects().isEmpty());
    }

    public void testLoadThreadCount() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        configOut.write("<system><configuration>"
                + "<threads count=\"2\"/>"
                + "</configuration></system>");
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertTrue(ccController.getProjects().isEmpty());
    }

    public void testLoadSomeProjects() throws IOException, CruiseControlException {
        ccController = new MockCruiseControlController();

        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(2, ccController.getProjects().size());
    }

    public void testLoadSomeProjectsWithDuplicates() throws IOException, CruiseControlException {
        ccController = new MockCruiseControlController();

        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject1");
        writeFooterAndClose(configOut);

        try {
            ccController.setConfigFile(configFile);
            fail("duplicate project names should fail");
        } catch (CruiseControlException expected) {
            assertEquals("Duplicate entries in config file for project name testProject1", expected.getMessage());
        }
    }

    // FIXME this is a test for the XMLConfigManager
    public void testLoadSomeProjectsWithParametrizedNames() throws IOException, CruiseControlException {
        ccController = new MockCruiseControlController();

        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        // a property that defines the project name.
        configOut.write("  <property name='name' value='testProject'/>\n");
        configOut.write("  <property name='encoding' value='utf8'/>\n");
        // this to test that plugin preconfiguration still works
        configOut.write("  <plugin name='testlistener' "
            + "classname='net.sourceforge.cruisecontrol.listeners.ListenerTestPlugin' "
            + "string='listener for ${project.name}'/>\n");

        // this to test that project name can be parametrized
        configOut.write("  <project name='${name}1'>\n");
        configOut.write("    <modificationset><alwaysbuild/></modificationset>\n");
        configOut.write("    <schedule><ant/></schedule>\n");
        configOut.write("    <listeners><testlistener/></listeners>\n");
        configOut.write("    <log dir='logs/${project.name}' encoding='${encoding}'/>\n");
        configOut.write("  </project>\n");
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);
        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(1, ccController.getProjects().size());
        final Project project = ((Project) ccController.getProjects().get(0));
        assertEquals("project name can be resolved", "testProject1", project.getName());

        assertEquals("project name can be resolved", "testProject1", project.getLog().getProjectName());

        List listeners = project.getListeners();
        assertEquals(1, listeners.size());
        Listener listener = (Listener) listeners.get(0);
        assertEquals(ListenerTestPlugin.class, listener.getClass());
        assertEquals("listener for testProject1", ((ListenerTestPlugin) listener).getString());

        assertEquals("logs/testProject1", project.getLogDir());
        assertEquals("utf8", project.getLog().getLogXmlEncoding());
    }

    public void testConfigReloading() throws IOException, CruiseControlException {
        MyListener listener = new MyListener();

        ccController = new MockCruiseControlController();

        ccController.addListener(listener);
        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);

        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(2, ccController.getProjects().size());
        assertEquals(2, listener.added.size());
        assertEquals(0, listener.removed.size());

        listener.clear();

        // no change - no reload
        ccController.parseConfigFileIfNecessary();
        // nothing happened
        assertEquals(0, listener.added.size());
        assertEquals(0, listener.removed.size());

        // add a project:

        listener.clear();

        sleep(1200);
        configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeProjectDetails(configOut, "testProject1");
        writeProjectDetails(configOut, "testProject2");
        writeProjectDetails(configOut, "testProject3");
        writeFooterAndClose(configOut);

        ccController.parseConfigFileIfNecessary();

        assertEquals(3, ccController.getProjects().size());
        assertEquals(1, listener.added.size());
        assertEquals(0, listener.removed.size());

        // remove 2 projects

        listener.clear();

        sleep(1200);
        configOut = new FileWriter(configFile);
        writeHeader(configOut);
        writeProjectDetails(configOut, "testProject3");
        writeFooterAndClose(configOut);

        ccController.reloadConfigFile();

        assertEquals(1, ccController.getProjects().size());
        assertEquals(0, listener.added.size());
        assertEquals(2, listener.removed.size());

    }

    public void testConfigReloadingWithXmlInclude() throws IOException, CruiseControlException {
        MyListener listener = new MyListener();

        ccController = new MockCruiseControlController();

        ccController.addListener(listener);

        FileWriter configOut2 = new FileWriter(configFile2);
        writeProjectDetails(configOut2, "testProject1");
        writeProjectDetails(configOut2, "testProject2");
        configOut2.close();

        FileWriter wrapperConfigOut = new FileWriter(configFile);
        wrapperConfigOut.write("<?xml version=\"1.0\" ?>\n");
        wrapperConfigOut.write("<!DOCTYPE cruisecontrol [ \n");
        wrapperConfigOut.write("<!ENTITY projects SYSTEM \"" + configFile2.getName() + "\"> \n");
        wrapperConfigOut.write("]> \n");
        wrapperConfigOut.write("<cruisecontrol>\n");
        wrapperConfigOut.write("&projects;");
        writeFooterAndClose(wrapperConfigOut);

        ccController.setConfigFile(configFile);

        assertEquals(configFile, ccController.getConfigFile());
        assertEquals(2, ccController.getProjects().size());
        assertEquals(2, listener.added.size());
        assertEquals(0, listener.removed.size());

        listener.clear();

        // no change - no reload
        ccController.parseConfigFileIfNecessary();
        // nothing happened
        assertEquals(0, listener.added.size());
        assertEquals(0, listener.removed.size());

        // add a project:

        listener.clear();

        sleep(1200);
        configOut2 = new FileWriter(configFile2);
        writeProjectDetails(configOut2, "testProject1");
        writeProjectDetails(configOut2, "testProject2");
        writeProjectDetails(configOut2, "testProject3");
        configOut2.close();

        ccController.parseConfigFileIfNecessary();

        assertEquals(3, ccController.getProjects().size());
        assertEquals(1, listener.added.size());
        assertEquals(0, listener.removed.size());

        // remove 2 projects

        listener.clear();

        sleep(1200);
        configOut2 = new FileWriter(configFile2);
        writeProjectDetails(configOut2, "testProject3");
        configOut2.close();

        ccController.reloadConfigFile();

        assertEquals(1, ccController.getProjects().size());
        assertEquals(0, listener.added.size());
        assertEquals(2, listener.removed.size());
    }

    public void testReadProject() {
        String tempDir = System.getProperty("java.io.tmpdir");
        Project project = ccController.readProject(tempDir);
        assertNotNull(project);
        assertTrue(project.getBuildForced());
    }

    public void testRegisterPlugins() throws IOException, CruiseControlException {
        FileWriter configOut = new FileWriter(configFile);
        writeHeader(configOut);
        configOut.write("  <plugin name='testname' "
                        + "classname='net.sourceforge.cruisecontrol.CruiseControllerTest'/>\n");
        configOut.write("  <plugin name='labelincrementer' classname='my.global.Incrementer'/>\n");
        writeFooterAndClose(configOut);

        ccController.setConfigFile(configFile);
        XMLConfigManager configManager = (XMLConfigManager) ccController.getConfigManager();
        CruiseControlConfig config = configManager.getCruiseControlConfig();

        PluginRegistry newRegistry = config.getRootPlugins();
        assertTrue(newRegistry.isPluginRegistered("testname"));
        assertFalse(newRegistry.isPluginRegistered("unknown_plugin"));
        assertEquals(newRegistry.getPluginClassname("labelincrementer"), "my.global.Incrementer");
    }

    private void writeHeader(FileWriter configOut) throws IOException {
        configOut.write("<?xml version=\"1.0\" ?>\n");
        configOut.write("<cruisecontrol>\n");
    }

    private void writeFooterAndClose(FileWriter configOut) throws IOException {
        configOut.write("</cruisecontrol>\n");
        configOut.close();
    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException dontCare) {
            System.out.println("dontCare happened");
        }
    }

    private void writeProjectDetails(FileWriter configOut, final String projectName) throws IOException {
        configOut.write("<project name='" + projectName + "'>\n");
        configOut.write("  <modificationset><alwaysbuild/></modificationset>\n");
        configOut.write("  <schedule><ant/></schedule>\n");
        configOut.write("</project>\n");
    }

    class MyListener implements CruiseControlController.Listener {
        private List added = new ArrayList();
        private List removed = new ArrayList();
        public void clear() {
            added.clear();
            removed.clear();
        }
        public void projectAdded(Project project) {
            added.add(project);
        }
        public void projectRemoved(Project project) {
            removed.add(project);
        }
    }
}
