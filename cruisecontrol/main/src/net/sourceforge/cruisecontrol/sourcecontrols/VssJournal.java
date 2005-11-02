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
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * This class handles all VSS-related aspects of determining the modifications since the last good build.
 * 
 * This class uses Source Safe Journal files. Unlike the history files that are generated by executing
 * <code>ss.exe history</code>, journal files must be setup by the Source Safe administrator before the point that
 * logging of modifications is to occur.
 * 
 * This code has been tested against Visual Source Safe v6.0 build 8383.
 * 
 * @author Eli Tucker
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Arun Aggarwal
 * @author Jonny Boman
 */
public class VssJournal implements SourceControl {

    private static final Logger LOG = Logger.getLogger(VssJournal.class);

    public static final SimpleDateFormat VSS_OUT_FORMAT = new SimpleDateFormat("'Date: 'MM/dd/yy  'Time: 'hh:mma",
            Locale.US);

    private String ssDir = "$/";
    private String journalFile;

    private Hashtable properties = new Hashtable();
    private String property;
    private String propertyOnDelete;

    private Date lastBuild;

    private ArrayList modifications = new ArrayList();
    private List moListVssJournalDateFormat = new ArrayList();

    public VssJournal() {
        // Add the default date format
        VssJournalDateFormat oVssJournalDateFormat = createVssjournaldateformat();
        oVssJournalDateFormat.setFormat("MM/dd/yy hh:mma");
    }

    /**
     * Add a nested element for date format interpretation from the journal file. The date and time parameters are fed
     * as a "date time" string (a single space separates date from time)
     * 
     * @return VssJournalDateFormat
     */
    public VssJournalDateFormat createVssjournaldateformat() {
        VssJournalDateFormat oVssJournalDateFormat = new VssJournalDateFormat();
        moListVssJournalDateFormat.add(oVssJournalDateFormat);
        return oVssJournalDateFormat;
    }

    /**
     * Set the project to get history from
     * 
     * @param ssDir
     */
    public void setSsDir(String ssDir) {
        this.ssDir = "$" + ssDir;
    }

    /**
     * Full path to journal file. Example: <code>c:/vssdata/journal/journal.txt</code>
     * 
     * @param journalFile
     */
    public void setJournalFile(String journalFile) {
        this.journalFile = journalFile;
    }

    /**
     * Choose a property to be set if the project has modifications if we have a change that only requires repackaging,
     * i.e. jsp, we don't need to recompile everything, just rejar.
     * 
     * @param property
     */
    public void setProperty(String property) {
        this.property = property;
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        this.propertyOnDelete = propertyOnDelete;
    }

    /**
     * Sets the _lastBuild date. Protected so it can be used by tests.
     */
    protected void setLastBuildDate(Date lastBuild) {
        this.lastBuild = lastBuild;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(journalFile, "journalfile", this.getClass());
        ValidationHelper.assertIsSet(ssDir, "ssdir", this.getClass());
    }

    /**
     * Do the work... I'm writing to a file since VSS will start wrapping lines if I read directly from the stream.
     */
    public List getModifications(Date lastBuild, Date now) {
        this.lastBuild = lastBuild;
        modifications.clear();

        try {
            final BufferedReader br = new BufferedReader(new FileReader(journalFile));
            try {
                String s = br.readLine();
                while (s != null) {
                    ArrayList entry = new ArrayList();
                    entry.add(s);
                    s = br.readLine();
                    while (s != null && !s.equals("")) {
                        entry.add(s);
                        s = br.readLine();
                    }
                    Modification mod = handleEntry(entry);
                    if (mod != null) {
                        modifications.add(mod);
                    }

                    if ("".equals(s)) {
                        s = br.readLine();
                    }
                }
            } finally {
                br.close();
            }
        } catch (Exception e) {
            LOG.warn(e);
        }

        if (property != null && modifications.size() > 0) {
            properties.put(property, "true");
        }

        LOG.info("Found " + modifications.size() + " modified files");
        return modifications;
    }

    /**
     * Parse individual VSS history entry
     * 
     * @param historyEntry
     */
    protected Modification handleEntry(List historyEntry) {
        Modification mod = new Modification("vss");
        String nameAndDateLine = (String) historyEntry.get(2);
        mod.userName = parseUser(nameAndDateLine);
        mod.modifiedTime = parseDate(nameAndDateLine);

        String folderLine = (String) historyEntry.get(0);
        String fileLine = (String) historyEntry.get(3);

        if (!isInSsDir(folderLine)) {
            // We are only interested in modifications to files in the specified ssdir
            return null;
        } else if (isBeforeLastBuild(mod.modifiedTime)) {
            // We are only interested in modifications since the last build
            return null;
        } else if (fileLine.startsWith("Labeled")) {
            // We don't add labels.
            return null;
        } else if (fileLine.startsWith("Checked in")) {

            String fileName = substringFromLastSlash(folderLine);
            String folderName = substringToLastSlash(folderLine);
            Modification.ModifiedFile modfile = mod.createModifiedFile(fileName, folderName);

            modfile.action = "checkin";
            mod.comment = parseComment(historyEntry);
        } else if (fileLine.indexOf(" renamed to ") > -1) {
            // TODO: This is a special case that is really two modifications: deleted and recovered.
            // For now I'll consider it a deleted to force a clean build.
            // I should really make this two modifications.
            mod.comment = parseComment(historyEntry);

            String fileName = fileLine.substring(0, fileLine.indexOf(" "));
            String folderName = folderLine;

            Modification.ModifiedFile modfile = mod.createModifiedFile(fileName, folderName);
            modfile.action = "delete";

        } else if (fileLine.indexOf(" moved to ") > -1) {
            // TODO: This is a special case that is really two modifications: deleted and recovered.
            // For now I'll consider it a deleted to force a clean build.
            // I should really make this two modifications.
            mod.comment = parseComment(historyEntry);
            String fileName = fileLine.substring(0, fileLine.indexOf(" "));
            String folderName = folderLine;

            Modification.ModifiedFile modfile = mod.createModifiedFile(fileName, folderName);
            modfile.action = "delete";

        } else {
            String folderName = folderLine;
            String fileName = fileLine.substring(0, fileLine.lastIndexOf(" "));
            Modification.ModifiedFile modfile = mod.createModifiedFile(fileName, folderName);

            mod.comment = parseComment(historyEntry);

            if (fileLine.endsWith("added")) {
                modfile.action = "add";
            } else if (fileLine.endsWith("deleted")) {
                modfile.action = "delete";
            } else if (fileLine.endsWith("recovered")) {
                modfile.action = "recover";
            } else if (fileLine.endsWith("shared")) {
                modfile.action = "branch";
            }
        }

        if (propertyOnDelete != null && "delete".equals(mod.type)) {
            properties.put(propertyOnDelete, "true");
        }

        if (property != null) {
            properties.put(property, "true");
        }

        return mod;
    }

    /**
     * parse comment from vss history (could be multiline)
     * 
     * @param a
     * @return the comment
     */
    private String parseComment(List a) {
        StringBuffer comment = new StringBuffer();
        for (int i = 4; i < a.size(); i++) {
            comment.append(a.get(i) + " ");
        }
        return comment.toString().trim();
    }

    /**
     * Parse date/time from VSS file history
     * 
     * The nameAndDateLine will look like User: Etucker Date: 6/26/01 Time: 11:53a Sometimes also this User: Aaggarwa
     * Date: 6/29/:1 Time: 3:40p Note the ":" instead of a "0"
     * 
     * May give additional DateFormats through the vssjournaldateformat tag. E.g.
     * <code><vssjournaldateformat format="yy-MM-dd hh:mm"/></code>
     * 
     * @return Date
     * @param nameAndDateLine
     */
    public Date parseDate(String nameAndDateLine) {
        // Extract date and time into one string with just one space separating the date from the time
        String dateAndTime = nameAndDateLine.substring(nameAndDateLine.indexOf("Date: ")).trim();
        // Fixup for weird format
        int indexOfColon = dateAndTime.indexOf("/:");
        if (indexOfColon != -1) {
            dateAndTime = dateAndTime.substring(0, indexOfColon)
                    + dateAndTime.substring(indexOfColon, indexOfColon + 2).replace(':', '0')
                    + dateAndTime.substring(indexOfColon + 2);
        }
        try {
            Date lastModifiedDate = VSS_OUT_FORMAT.parse(dateAndTime + "m");

            return lastModifiedDate;
        } catch (ParseException pe) {
            // The standard parsing failed so we see if there are any suggestions
            // on how to interpret the date, but first we extract date and time into one
            // string with just one space separating the date from the time
            dateAndTime = dateAndTime.substring(5);
            String sDate = dateAndTime.substring(0, dateAndTime.indexOf("Time:")).trim();
            String sTime = dateAndTime.substring(dateAndTime.indexOf("Time:") + 5).trim();
            dateAndTime = sDate + " " + sTime;
            Date oDate = null;
            for (Iterator oIterator = moListVssJournalDateFormat.iterator(); oIterator.hasNext();) {
                VssJournalDateFormat oVssJournalDateFormat = (VssJournalDateFormat) oIterator.next();
                try {
                    oDate = oVssJournalDateFormat.getDateFormat().parse(dateAndTime);
                } catch (ParseException e) {
                    // No luck with this one
                }
            }
            if (oDate == null) {
                LOG.error("Could not parse date in VssJournal file");
            }
            return oDate;
        }
    }

    /**
     * Parse username from VSS file history
     * 
     * @param userLine
     * @return the user name who made the modification
     */
    public String parseUser(String userLine) {
        final int startOfUserName = 6;

        try {
            String userName = userLine.substring(startOfUserName, userLine.indexOf("Date: ") - 1).trim();

            return userName;
        } catch (StringIndexOutOfBoundsException e) {
            LOG.error("Unparsable string was: " + userLine);
            throw e;
        }

    }

    /**
     * Returns the substring of the given string from the last "/" character. UNLESS the last slash character is the
     * last character or the string does not contain a slash. In that case, return the whole string.
     */
    public String substringFromLastSlash(String input) {
        int lastSlashPos = input.lastIndexOf("/");
        if (lastSlashPos > 0 && lastSlashPos + 1 <= input.length()) {
            return input.substring(lastSlashPos + 1);
        }
        
        return input;
    }

    /**
     * Returns the substring of the given string from the beginning to the last "/" character or till the end of the
     * string if no slash character exists.
     */
    public String substringToLastSlash(String input) {
        int lastSlashPos = input.lastIndexOf("/");
        if (lastSlashPos > 0) {
            return input.substring(0, lastSlashPos);
        }
        
        return input;
    }

    /**
     * Determines if the given folder is in the ssdir specified for this VssJournalElement.
     */
    protected boolean isInSsDir(String path) {
        boolean isInDir = (path.toLowerCase().indexOf(ssDir.toLowerCase()) != -1);
        if (isInDir) {
            // exclude similarly prefixed paths
            if (ssDir.equalsIgnoreCase(path) // is exact same as ssDir (this happens)
                    || ('/' == path.charAt(ssDir.length())) // subdirs below matching ssDir
                    || "$/".equalsIgnoreCase(ssDir)) { // everything is included

                // do nothing
            } else {
                // is not really in subdir
                isInDir = false;
            }
        }
        return isInDir;
    }

    /**
     * Determines if the date given is before the last build for this VssJournalElement.
     */
    protected boolean isBeforeLastBuild(Date date) {
        return date.before(lastBuild);
    }

    public static class VssJournalDateFormat {
        private DateFormat moDateFormat;
        private String msFormat;

        public void setFormat(String psFormat) {
            moDateFormat = new SimpleDateFormat(psFormat);
            msFormat = psFormat;
        }

        public String getFormat() {
            return msFormat;
        }

        public final DateFormat getDateFormat() {
            return moDateFormat;
        }

    }

}
