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

package net.sourceforge.cruisecontrol;

import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class Modification implements Comparable {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(Modification.class);

    public String type = "unknown";
    public String fileName;
    public String folderName;
    public Date modifiedTime;
    public String userName;
    public String emailAddress;
    public String comment = "";

    public Element toElement(DateFormat formatter) {
        Element modificationElement = new Element("modification");
        modificationElement.setAttribute("type", type);
        Element filenameElement = new Element("filename");
        filenameElement.addContent(fileName);
        Element projectElement = new Element("project");
        projectElement.addContent(folderName);
        Element dateElement = new Element("date");
        dateElement.addContent(formatter.format(modifiedTime));
        Element userElement = new Element("user");
        userElement.addContent(userName);
        Element commentElement = new Element("comment");

        CDATA cd = null;
        try {
            cd = new CDATA(comment);
        } catch (org.jdom.IllegalDataException e) {
            log.error(e);
            cd = new CDATA("Unable to parse comment.  It contains illegal data.");
        }
        commentElement.addContent(cd);

        modificationElement.addContent(filenameElement);
        modificationElement.addContent(projectElement);
        modificationElement.addContent(dateElement);
        modificationElement.addContent(userElement);
        modificationElement.addContent(commentElement);

        // not all sourcecontrols guarantee a non-null email address
        if ( emailAddress != null ) {
            Element emailAddressElement = new Element("email");
            emailAddressElement.addContent(emailAddress);
            modificationElement.addContent(emailAddressElement);
        }

        return modificationElement;
    }

    public String toXml(DateFormat formatter) {
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(toElement(formatter));
    }

    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        StringBuffer sb = new StringBuffer();
        sb.append("FileName: " + fileName + "\n");
        sb.append("FolderName: " + folderName + "\n");
        sb.append("Last Modified: " + formatter.format(modifiedTime) + "\n");
        sb.append("UserName: " + userName + "\n");
        sb.append("EmailAddress: " + emailAddress + "\n");
        sb.append("Comment: " + comment + "\n");
        return sb.toString();
    }

    public void log(DateFormat formatter) {
        log.debug("FileName: " + fileName);
        log.debug("FolderName: " + folderName);
        log.debug("Last Modified: " + formatter.format(modifiedTime));
        log.debug("UserName: " + userName);
        log.debug("EmailAddress: " + emailAddress);
        log.debug("Comment: " + comment);
        log.debug("");
    }

    public int compareTo(Object o) {
        Modification modification = (Modification) o;
        return modifiedTime.compareTo(modification.modifiedTime);
    }

    public boolean equals(Object o) {
        if(o == null)
            return false;

        if(!(o instanceof Modification))
            return false;

        Modification mod = (Modification) o;

        boolean emailsAreEqual = ((emailAddress == null && mod.emailAddress == null) || emailAddress.equals(mod.emailAddress));

        return (type.equals(mod.type) &&
            fileName.equals(mod.fileName) &&
            folderName.equals(mod.folderName) &&
            modifiedTime.equals(mod.modifiedTime) &&
            userName.equals(mod.userName) &&
            emailsAreEqual &&
            comment.equals(mod.comment));
    }

    //for brief testing only
    public static void main(String args[]) {
        Date now = new Date();
        Modification mod = new Modification();
        mod.fileName = "File\"Name&";
        mod.folderName = "Folder'Name";
        mod.modifiedTime = now;
        mod.userName = "User<>Name";
        mod.comment = "Comment";
        System.out.println(mod.toXml(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")));
    }
}