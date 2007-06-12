/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.dashboard;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.cruisecontrol.dashboard.utils.DashboardConfig;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Projects {
    private File baseLogs;

    private File base;

    private Set projectNames;

    private File baseArtifacts;

    public Projects(File basedir, DashboardConfig cruiseControlConfig) {
        base = basedir;
        baseLogs = new File(basedir, "logs");
        baseArtifacts = new File(base, "artifacts");
        projectNames = cruiseControlConfig.getProjectNames();
    }

    public boolean hasProject(String projectName) {
        return projectNames.contains(projectName);
    }

    public File getArtifactRoot(String projectName) {
        File artifacts = new File(baseArtifacts, projectName);
        return artifacts.exists() ? artifacts : null;
    }

    public File getLogRoot(String projectName) {
        return new File(baseLogs, projectName);
    }

    public File[] getProjectNames() {
        File[] projects = new File[projectNames.size()];
        int i = 0;
        for (Iterator iter = projectNames.iterator(); iter.hasNext();) {
            projects[i++] = getLogRoot((String) iter.next());
        }
        return projects;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}