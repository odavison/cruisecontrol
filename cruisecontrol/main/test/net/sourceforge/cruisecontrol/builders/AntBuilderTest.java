/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol.builders;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jdom.Element;

public class AntBuilderTest extends TestCase {
    private final List filesToClear = new ArrayList();

    public void tearDown() {
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void testValidate() {
        AntBuilder ab = new AntBuilder();

        try {
            ab.validate();
        } catch (CruiseControlException e) {
            fail("antbuilder has no required attributes");
        }

        ab.setTime("0100");
        ab.setBuildFile("buildfile");
        ab.setTarget("target");

        try {
            ab.validate();
        } catch (CruiseControlException e) {
            fail("validate should not throw exceptions when options are set.");
        }
        
        ab.setMultiple(2);

        try {
            ab.validate();
            fail("validate should throw exceptions when multiple and time are both set.");
        } catch (CruiseControlException e) {
        }
    }

    public void testGetCommandLineArgs() {
        AntBuilder builder = new AntBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        Hashtable properties = new Hashtable();
        properties.put("label", "200.1.23");
        String classpath = System.getProperty("java.class.path");
        String[] resultDebug =
            {
                "java",
                "-classpath",
                classpath,
                "org.apache.tools.ant.Main",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-debug",
                "-verbose",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultInfo =
            {
                "java",
                "-classpath",
                classpath,
                "org.apache.tools.ant.Main",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultLogger =
            {
                "java",
                "-classpath",
                classpath,
                "org.apache.tools.ant.Main",
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultDebugWithMaxMemory =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                classpath,
                "org.apache.tools.ant.Main",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-debug",
                "-verbose",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultDebugWithMaxMemoryAndProperty =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                classpath,
                "org.apache.tools.ant.Main",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-Dfoo=bar",
                "-debug",
                "-verbose",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultBatchFile =
            {
                "cmd.exe",
                "/C",
                "ant.bat",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        String[] resultShellScript =
            {
                "ant.sh",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        BasicConfigurator.configure(
            new ConsoleAppender(new PatternLayout("%m%n")));

        assertTrue(
            Arrays.equals(
                resultInfo,
                builder.getCommandLineArgs(properties, false, false, false)));
        assertTrue(
            Arrays.equals(
                resultLogger,
                builder.getCommandLineArgs(properties, true, false, false)));

        builder.setAntScript("ant.bat");
        assertTrue(
            Arrays.equals(
                resultBatchFile,
                builder.getCommandLineArgs(properties, false, true, true)));
        builder.setAntScript("ant.sh");
        assertTrue(
            Arrays.equals(
                resultShellScript,
                builder.getCommandLineArgs(properties, false, true, false)));
        builder.setAntScript(null);

        builder.setUseDebug(true);
        assertTrue(
            Arrays.equals(
                resultDebug,
                builder.getCommandLineArgs(properties, false, false, false)));

        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) builder.createJVMArg();
        arg.setArg("-Xmx256m");
        assertTrue(
            Arrays.equals(
                resultDebugWithMaxMemory,
                builder.getCommandLineArgs(properties, false, false, false)));

        AntBuilder.Property prop = builder.createProperty();
        prop.setName("foo");
        prop.setValue("bar");
        assertTrue(
            Arrays.equals(
                resultDebugWithMaxMemoryAndProperty,
                builder.getCommandLineArgs(properties, false, false, false)));

    }

    public void testGetAntLogAsElement() throws Exception {
        try {
            Element buildLogElement = new Element("build");
            File logFile = new File("_tempAntLog14.xml");
            filesToClear.add(logFile);
            BufferedWriter bw1 = new BufferedWriter(new FileWriter(logFile));
            bw1.write(
                "<?xml:stylesheet type=\"text/xsl\" href=\"log.xsl\"?><build></build>");
            bw1.flush();
            bw1.close();
            File logFile2 = new File("_tempAntLog141.xml");
            filesToClear.add(logFile2);
            BufferedWriter bw2 = new BufferedWriter(new FileWriter(logFile2));
            bw2.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><?xml:stylesheet "
                    + "type=\"text/xsl\" href=\"log.xsl\"?><build></build>");
            bw2.flush();
            bw2.close();

            assertEquals(
                buildLogElement.toString(),
                AntBuilder.getAntLogAsElement(logFile).toString());
            assertEquals(
                buildLogElement.toString(),
                AntBuilder.getAntLogAsElement(logFile2).toString());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void testBuild() throws Exception {
        AntBuilder builder = new AntBuilder();
        builder.setBuildFile("build.xml");
        builder.setTempFile("notLog.xml");
        builder.setTarget("init");
        HashMap buildProperties = new HashMap();
        Element buildElement = builder.build(buildProperties);
        int initCount = getInitCount(buildElement);
        assertEquals(1, initCount);

        builder.setTarget("init init");
        buildElement = builder.build(buildProperties);
        initCount = getInitCount(buildElement);
        assertEquals(2, initCount);
    }

    public int getInitCount(Element buildElement) {
        int initFoundCount = 0;
        Iterator targetIterator = buildElement.getChildren("target").iterator();
        String name;
        while (targetIterator.hasNext()) {
            name = ((Element) targetIterator.next()).getAttributeValue("name");
            if (name.equals("init")) {
                initFoundCount++;
            }
        }
        return initFoundCount;
    }
}
