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

import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  Instantiates a project from a JDOM Element
 */
public class ProjectXMLHelper {

    private static final org.apache.log4j.Logger LOG4J =
            org.apache.log4j.Logger.getLogger(ProjectXMLHelper.class);

    private PluginRegistry plugins;
    private Element projectElement;
    private String projectName;

    public ProjectXMLHelper() throws CruiseControlException {
        plugins = PluginRegistry.getDefaultPluginRegistry();
    }

    public ProjectXMLHelper(File configFile, String projName) throws CruiseControlException {
        this();
        Iterator projectIterator =
                Util.loadConfigFile(configFile).getChildren("project").iterator();
        while (projectIterator.hasNext()) {
            Element currentProjectElement = (Element) projectIterator.next();
            if (currentProjectElement.getAttributeValue("name") != null
                    && currentProjectElement.getAttributeValue("name").equals(projName)) {
                projectElement = currentProjectElement;
            }
        }
        if (projectElement == null) {
            throw new CruiseControlException("Project not found in config file: " + projName);
        }

        projectName = projName;
        setDateFormat(projectElement);

        Iterator pluginIterator = projectElement.getChildren("plugin").iterator();
        while (pluginIterator.hasNext()) {
            Element pluginElement = (Element) pluginIterator.next();
            String pluginName = pluginElement.getAttributeValue("name");
            String pluginClassName = pluginElement.getAttributeValue("classname");
            if (pluginName == null || pluginClassName == null) {
                throw new CruiseControlException("name and classname are required on <plugin>");
            }
            LOG4J.debug("Registering plugin: " + pluginName);
            LOG4J.debug("to classname: " + pluginClassName);
            LOG4J.debug("");
            plugins.register(pluginName, pluginClassName);
        }
    }

    protected void setDateFormat(Element projElement) {
        if (projElement.getChild("dateformat") != null
                && projElement.getChild("dateformat").getAttributeValue("format") != null) {
            DateFormatFactory.setFormat(
                    projElement.getChild("dateformat").getAttributeValue("format"));
        }
    }

    public boolean getBuildAfterFailed() {
        String buildAfterFailedAttr = projectElement.getAttributeValue("buildafterfailed");
        if (!"false".equalsIgnoreCase(buildAfterFailedAttr)) {
            // default if not specified and all other cases
            buildAfterFailedAttr = "true";
        }
        boolean buildafterfailed = Boolean.valueOf(buildAfterFailedAttr).booleanValue();
        LOG4J.debug("Setting BuildAfterFailed to " + buildafterfailed);
        return buildafterfailed;
    }

    public List getBootstrappers() throws CruiseControlException {
        List bootstrappers = new ArrayList();
        Element element = projectElement.getChild("bootstrappers");
        if (element != null) {
            Iterator bootstrapperIterator = element.getChildren().iterator();
            while (bootstrapperIterator.hasNext()) {
                Element bootstrapperElement = (Element) bootstrapperIterator.next();
                Bootstrapper bootstrapper =
                        (Bootstrapper) configurePlugin(bootstrapperElement, false);
                bootstrapper.validate();
                bootstrappers.add(bootstrapper);
            }
        } else {
            LOG4J.debug("Project " + projectName + " has no bootstrappers");
        }
        return bootstrappers;
    }

    public List getPublishers() throws CruiseControlException {
        List publishers = new ArrayList();
        Element publishersElement = projectElement.getChild("publishers");
        if (publishersElement != null) {
            Iterator publisherIterator = publishersElement.getChildren().iterator();
            while (publisherIterator.hasNext()) {
                Element publisherElement = (Element) publisherIterator.next();
                Publisher publisher = (Publisher) configurePlugin(publisherElement, false);
                publisher.validate();
                publishers.add(publisher);
            }
        } else {
            LOG4J.debug("Project " + projectName + " has no publishers");
        }
        return publishers;
    }

    public Schedule getSchedule() throws CruiseControlException {
        Element scheduleElement = getRequiredElement(projectElement, "schedule");
        Schedule schedule = (Schedule) configurePlugin(scheduleElement, true);
        Iterator builderIterator = scheduleElement.getChildren().iterator();
        while (builderIterator.hasNext()) {
            Element builderElement = (Element) builderIterator.next();
            // TODO: PauseBuilder should be able to be handled like any other
            // Builder
            if (builderElement.getName().equalsIgnoreCase("pause")) {
                PauseBuilder pauseBuilder = (PauseBuilder) configurePlugin(builderElement, false);
                pauseBuilder.validate();
                schedule.addPauseBuilder(pauseBuilder);
            } else {
                Builder builder = (Builder) configurePlugin(builderElement, false);
                builder.validate();
                schedule.addBuilder(builder);
            }
        }
        schedule.validate();

        return schedule;
    }

    public ModificationSet getModificationSet() throws CruiseControlException {
        final Element modSetElement = getRequiredElement(projectElement, "modificationset");
        ModificationSet modificationSet = (ModificationSet) configurePlugin(modSetElement, true);
        Iterator sourceControlIterator = modSetElement.getChildren().iterator();
        while (sourceControlIterator.hasNext()) {
            Element sourceControlElement = (Element) sourceControlIterator.next();
            SourceControl sourceControl =
                    (SourceControl) configurePlugin(sourceControlElement, false);
            sourceControl.validate();
            modificationSet.addSourceControl(sourceControl);
        }
        modificationSet.validate();
        return modificationSet;
    }

    public LabelIncrementer getLabelIncrementer() throws CruiseControlException {
        LabelIncrementer incrementer;
        Element labelIncrementerElement = projectElement.getChild("labelincrementer");
        if (labelIncrementerElement != null) {
            incrementer = (LabelIncrementer) configurePlugin(labelIncrementerElement, false);
        } else {
            Class labelIncrClass = plugins.getPluginClass("labelincrementer");
            try {
                incrementer = (LabelIncrementer) labelIncrClass.newInstance();
            } catch (Exception e) {
                LOG4J.error(
                        "Error instantiating label incrementer named "
                        + labelIncrClass.getName()
                        + ". Using DefaultLabelIncrementer instead.",
                        e);
                incrementer = new DefaultLabelIncrementer();
            }
        }
        return incrementer;
    }

    /**
     *  returns the String value of an attribute on an element, exception if it's not set
     */
    protected String getRequiredAttribute(Element element, String attributeName)
            throws CruiseControlException {
        if (element.getAttributeValue(attributeName) != null) {
            return element.getAttributeValue(attributeName);
        } else {
            throw new CruiseControlException(
                    "Project "
                    + projectName
                    + ":  attribute "
                    + attributeName
                    + " is required on "
                    + element.getName());
        }
    }

    private Element getRequiredElement(final Element parentElement, final String childName)
            throws CruiseControlException {
        final Element requiredElement = parentElement.getChild(childName);
        if (requiredElement == null) {
            throw new CruiseControlException(
                    "Project "
                    + projectName
                    + ": <"
                    + parentElement.getName()
                    + ">"
                    + " requires a <"
                    + childName
                    + "> element");
        }
        return requiredElement;
    }

    /**
     *  TODO: also check that instantiated class implements/extends correct interface/class
     */
    protected Object configurePlugin(Element pluginElement, boolean skipChildElements)
            throws CruiseControlException {
        String name = pluginElement.getName();
        PluginXMLHelper pluginHelper = new PluginXMLHelper();
        String pluginName = pluginElement.getName();

        if (plugins.isPluginRegistered(pluginName)) {
            return pluginHelper.configure(
                    pluginElement,
                    plugins.getPluginClass(pluginName),
                    skipChildElements);
        } else {
            throw new CruiseControlException("Unknown plugin for: <" + name + ">");
        }
    }

    /**
     * Returns a Log instance representing the Log element.
     */
    public Log getLog() throws CruiseControlException {
        Log log = new Log(this.projectName);

        //Init the log dir to the default.
        String defaultLogDir = "logs" + File.separatorChar + projectName;
        log.setLogDir(defaultLogDir);

        Element logElement = projectElement.getChild("log");
        if (logElement != null) {
            String logDirValue = logElement.getAttributeValue("dir");
            //The user has specified a different log dir, so set
            //  that one instead.
            if (logDirValue != null) {
                log.setLogDir(logDirValue);
            }
            log.setLogXmlEncoding(logElement.getAttributeValue("encoding"));


            //Get the BuildLoggers...all the children of the Log element should be
            //  BuildLogger implementations
            Iterator loggerIter = logElement.getChildren().iterator();
            while (loggerIter.hasNext()) {
                Element nextLoggerElement = (Element) loggerIter.next();

                BuildLogger nextLogger =
                        (BuildLogger) configurePlugin(nextLoggerElement, false);
                nextLogger.validate();

                log.addLogger(nextLogger);
            }
        }

        return log;
    }
}
