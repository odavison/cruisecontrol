package net.sourceforge.cruisecontrol.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class provides helper methods for interacting with Input/Output classes.
 */
public final class IO {

    private IO() {
        //Helper methods only.
    }

    public static void close(OutputStream o) {
        if (o != null) {
            try {
                o.close();
            } catch (IOException ignored) {
                // nevermind, then
            }
        }
    }

    public static void close(InputStream i) {
        if (i != null) {
            try {
                i.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void close(Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(Writer w) {
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(Process p) {
        try {
            close(p.getInputStream());
            close(p.getOutputStream());
            close(p.getErrorStream());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    public static void output(File to, Element xml, String encoding) throws CruiseControlException {
        OutputStream logStream = null;
        try {
            Format format = Format.getPrettyFormat();
            if (encoding != null) {
                format.setEncoding(encoding);
            }
            XMLOutputter outputter = new XMLOutputter(format);
            logStream = new BufferedOutputStream(new FileOutputStream(to));
            outputter.output(new Document(xml), logStream);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            close(logStream);
        }
    }

    /**
     * Deletes a File instance. If the file represents a directory, all
     * the subdirectories and files within.
     */
    public static void delete(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            deleteDir(f);
            return;
        }
        f.delete();
    }

    private static void deleteDir(File dir) {
        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            delete(child);
        }
        dir.delete();
    }

    public static void delete(File f, boolean debuggerOn, Logger log) {
        try {
            delete(f);
            if (debuggerOn) {
                log.info("Removed temp file " + f.getAbsolutePath());
            }
        } catch (Exception ignored) {
            //never mind
        }
    }

    /**
     * Writes the contents of a file to a PrintStream.
     */
    public static void dumpTo(File f, PrintStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            while (in.ready()) {
                out.println(in.readLine());
            }
        } catch (Exception ignored) {
        } finally {
            close(in);
        }
    }

    /**
     * Write the content to the file.
     */
    public static void write(String fileName, String content) throws CruiseControlException {
        write(new File(fileName), content);
    }

    /**
     * Write the content to the file.
     */
    public static void write(File f, String contents) throws CruiseControlException {
        FileWriter fw = null;
        try {
            f.getParentFile().mkdirs();
            fw = new FileWriter(f);
            fw.write(contents);
        } catch (IOException ioe) {
            throw new CruiseControlException("Error writing file: " + f.getAbsolutePath(), ioe);
        } finally {
            close(fw);
        }
    }

    /**
     * @return List of lines of text (String objects)
     */
    public static List readLines(File source)
        throws CruiseControlException {

        List result = new ArrayList();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(source));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException("Error reading file: " + source.getAbsolutePath(), ioe);
        } finally {
            close(reader);
        }

        return result;
    }

}
