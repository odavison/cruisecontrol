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
package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.ProjectXMLHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.util.Map;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * <p>The <code>&lt;property&gt;</code> element is used to set a property (or set of properties)
 * within the CruiseControl configuration file. Properties may be set at the global level
 * and/or within the scope of a project. There are three ways to set properties within CruiseControl:</p>
 *
 * <ol>
 *     <li>By supplying both the name and value attributes.</li>
 *     <li>By setting the file attribute with the filename of the property file to load.
 *         This property file must follow the format defined by the class java.util.Properties,
 *         with the same rules about how non-ISO8859-1 characters must be escaped.</li>
 *     <li>By setting the environment attribute with a prefix to use. Properties will be defined
 *         for every environment variable by prefixing the supplied name and a period to the name
 *         of the variable.</li>
 *
 * </ol>
 *
 * <p>Properties in CruiseControl are <i>not entirely</i> immutable: whoever sets a property <i>last</i>
 * will freeze it's value <i>within the scope in which the property was set</i>. In other words,
 * you may define a property at the global level, then eclipse this value within the scope of a single
 * project by redefining the property within that project. You may not, however, set a property more
 * than once within the same scope. If you do so, only the last assignment will be used.</p>
 *
 * <p>Just as in Ant, the value part of a property being set may contain references to other properties.
 * These references are resolved at the time these properties are set. This also holds for properties
 * loaded from a property file, or from the environment.</p>
 *
 * <p>Also note that the property <code>${project.name}</code> is set for you automatically and will always resolve
 * to the name of the project currently being serviced - even outside the scope of the project
 * definition.</p>
 *
 * <p>Finally, note that properties bring their best when combined with
 * <a href=\"plugins.html#preconfiguration\">plugin preconfigurations</a>.
 * </p>
 */
public class DefaultPropertiesPlugin implements PropertiesPlugin {
   private String file;
   private String environment;
   private String name;
   private String value;
   private String toupper;

  /**
   * The name of the property to set.
   * @required Exactly one of name, environment, or file.
   */
  public void setName(String name) {
    this.name = name;
  }


  /**
   * The prefix to use when retrieving environment variables.
   * Thus if you specify environment="myenv" you will be able to access OS-specific environment variables
   * via property names such as "myenv.PATH" or "myenv.MAVEN_HOME".
   * @required Exactly one of name, environment, or file.
   */
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  /**
   * The filename of the property file to load.
   * @required Exactly one of name, environment, or file.
   */
  public void setFile(String file) {
    this.file = file;
  }

  /**
   *
   * @param value
   * @required Yes, if name was set.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Used in conjunction with <code>environment</code>. If set to <code>true</code>, all
   * environment variable names will be converted to upper case.
   * @param toupper
   */
  public void setToupper(String toupper) {
    this.toupper = toupper;
  }
  /**
  * Called after the configuration is read to make sure that all the mandatory parameters were specified.. @throws
  * CruiseControlException if there was a configuration error.
  */
  public void validate() throws CruiseControlException {
      if (name == null && file == null && environment == null) {
        ValidationHelper.fail("At least one of name, file or environment must be set.");
      }
      if ((name != null && (file != null || environment != null)
       || (file != null && (name != null || environment != null))
       || (environment != null && (file != null || name != null)))) {
        ValidationHelper.fail("At most one of name, file or environment can be set.");
      }

      if (file != null && file.trim().length() > 0) {
          // TODO FIXME add exists check.
      }
      if (name != null && value == null) {
        ValidationHelper.fail("name and value must be set simultaneoulsy.");
      }
  }

  public void loadProperties(Map props, boolean failIfMissing) throws CruiseControlException {
    boolean toUpperValue = "true".equals(toupper);
    if (file != null && file.trim().length() > 0) {
        File theFile = new File(this.file);
        // TODO FIXME add exists check.
        try {
            BufferedReader reader = new BufferedReader(new FileReader(theFile));
            // Read the theFile line by line, expanding macros
            // as we go. We must do this manually to preserve the
            // order of the properties.
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                int index = line.indexOf('=');
                if (index < 0) {
                    continue;
                }
                String parsedName
                    = Util.parsePropertiesInString(props, line.substring(0, index).trim(), failIfMissing);
                String parsedValue
                    = Util.parsePropertiesInString(props, line.substring(index + 1).trim(), failIfMissing);
                ProjectXMLHelper.setProperty(props, parsedName, parsedValue);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            throw new CruiseControlException("Could not load properties from theFile \"" + this.file
                    + "\". The theFile does not exist", e);
        } catch (IOException e) {
            throw new CruiseControlException("Could not load properties from theFile \"" + this.file
                    + "\".", e);
        }
    } else if (environment != null) {
        // Load the environment into the project's properties
        Iterator variables = new OSEnvironment().getEnvironment().iterator();
        while (variables.hasNext()) {
            String line = (String) variables.next();
            int index = line.indexOf('=');
            if (index < 0) {
                continue;
            }
            // If the toupper attribute was set, upcase the variables
            StringBuffer propName = new StringBuffer(environment);
            propName.append(".");
            if (toUpperValue) {
                propName.append(line.substring(0, index).toUpperCase());
            } else {
                propName.append(line.substring(0, index));
            }
            String parsedValue
                    = Util.parsePropertiesInString(props, line.substring(index + 1), failIfMissing);
            ProjectXMLHelper.setProperty(props, propName.toString(), parsedValue);
        }
    } else {
        String parsedValue = Util.parsePropertiesInString(props, value, failIfMissing);
        ProjectXMLHelper.setProperty(props, name, parsedValue);
    }
  }
}
