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
package net.sourceforge.cruisecontrol.publishers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.builders.AntBuilder;

/**
 * A thin wrapper around the AntBuilder class, this class allows you to call an
 * Ant script as a publisher.
 * <p>
 * All properties set by CC and passed to the builder will be available as
 * properties within Ant.
 * 
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith </a>
 */
public class AntPublisher extends AntBuilder implements Publisher {
    
    private static final Logger LOG = Logger.getLogger(AntPublisher.class);
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(Element log) throws CruiseControlException {
        
        Map map = new HashMap();

        // Add CC properties to the AntBuilder
        Iterator propertyIterator = log.getChild("info").getChildren("property")
                .iterator();
        while (propertyIterator.hasNext()) {
            Element property = (Element) propertyIterator.next();
            map.put(property.getAttributeValue("name"), 
                    property.getAttributeValue("value"));
        }
        
        // Run Ant
        Element result = build(map);
        if (result == null) {
            LOG.error("Publisher failed.\n\n");
        } else {
            Attribute error = result.getAttribute("error");
            if (error == null) {
                LOG.info("Publisher successful.");
            } else {
                LOG.error("Publisher failed.\n\n" 
                        + error.getValue() 
                        + "\n");
            }         
        }
    }
}
