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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import junit.framework.*;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.CruiseControlException;

import javax.swing.event.ChangeListener;

/**
 *@author  Robert Watkins
 *@author  Jason Yip, jcyip@thoughtworks.com
 */
public class P4Test extends TestCase {

	public P4Test(String testName) {
		super(testName);
	}

    public void testParseChangelists() throws IOException, ParseException {
        File testLog = new File("test/net/sourceforge/cruisecontrol/sourcecontrols/p4_changes.txt");
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(testLog));

        P4 p4 = new P4();
        String[] changelists = p4.parseChangelistNumbers(input);
        input.close();
        assertNotNull("No changelists returned", changelists);
        assertEquals("Returned wrong number of changelists", 4, changelists.length);
        String[] expectedChangelists = new String[]{"14","12","11"};
        for (int i = 0; i < expectedChangelists.length; i++) {
            assertEquals("Returned wrong changelist number", expectedChangelists[i], changelists[i]);
        }
    }

    public void testParseChangeDescriptions() throws IOException, ParseException {
        File testLog = new File("test/net/sourceforge/cruisecontrol/sourcecontrols/p4_describe.txt");
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(testLog));

        P4 p4 = new P4();
        List changelists = p4.parseChangeDescriptions(input);
        input.close();
        assertEquals("Returned wrong number of changelists", 3, changelists.size());
    }

//    public void testGetModifications() throws Exception {
//        
//        // REAL TEST IF YOU NEED IT
//        P4 p4 = new P4();
//        p4.setView("//depot/...");
//        List changelists = p4.getModifications(new Date(0), new Date(), 0);
//        assertEquals("Returned wrong number of changelists", 3, changelists.size());
//        
//    }
        
	public static void main(String[] args) {
		junit.textui.TestRunner.run(P4Test.class);
	}

}
