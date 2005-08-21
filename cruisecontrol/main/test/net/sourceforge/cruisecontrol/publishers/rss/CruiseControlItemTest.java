/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
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
package net.sourceforge.cruisecontrol.publishers.rss;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

public class CruiseControlItemTest extends TestCase {


    private XMLLogHelper successLogHelper;
    private XMLLogHelper fixedLogHelper;
    private XMLLogHelper failureLogHelper;
    private XMLLogHelper firstFailureLogHelper;

    protected XMLLogHelper createLogHelper(boolean success, boolean lastBuildSuccess) {
        Element cruisecontrolElement = TestUtil.createElement(success, lastBuildSuccess);

        return new XMLLogHelper(cruisecontrolElement);
    }

    public void setUp() {
        successLogHelper = createLogHelper(true, true);
        failureLogHelper = createLogHelper(false, false);
        fixedLogHelper = createLogHelper(true, false);
        firstFailureLogHelper = createLogHelper(false, true);
    }


    public void testConstructors() throws Exception {
        CruiseControlItem item = new CruiseControlItem(successLogHelper, "link");

        assertEquals("someproject somelabel Build Successful", item.getTitle());
        assertEquals(
            "<em>Build Time:</em> not available<br/><em>Label:</em> somelabel<br/"
            + "><em>Modifications: </em>0<br/>\n<ul>",
            item.getDescription());

        // Can't validate the date published field because the TestUtil.createElement
        // method doesn't insert a build date, so we don't know what the date
        // should be...
        // FIXME

        //assertEquals("", item.getLink());
   }
}