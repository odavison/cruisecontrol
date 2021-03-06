/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;

/**
 * Class for executing a <code>Commandline</code>. It setups an extra thread to consume stderr and blocks until the
 * process and the extra thread has finished.
 */
public class CommandExecutor {
    private static final Logger LOG = Logger.getLogger(CommandExecutor.class);
    private StreamConsumer error = StreamLogger.getWarnLogger(LOG);
    private StreamConsumer output = StreamLogger.getInfoLogger(LOG);
    private final Commandline command;

    /**
     * Creates a new instance of CommandExecutor.
     * @param command command to be executed
     */
    public CommandExecutor(Commandline command) {
        this.command = command;
    }

    /**
     * Creates a new instance of CommandExecutor.
     * @param command command to be executed
     * @param log logger to which process streams should be written 
     */
    public CommandExecutor(Commandline command, Logger log) {
        this(command);
        logErrorStreamTo(log);
        logOutputStreamTo(log);
    }

    /**
     * Sends the process' error stream (stderr) to a log as warnings.
     *
     * @param log
     *            where to log the error stream.
     */
    public void logErrorStreamTo(Logger log) {
        error = StreamLogger.getWarnLogger(log);
    }

    /**
     * Sends the process' output stream (stdout) to a log as info.
     *
     * @param log
     *            where to log the output stream.
     */
    public void logOutputStreamTo(Logger log) {
        output = StreamLogger.getInfoLogger(log);
    }

    /**
     * Sends the process' output stream (stdout) to a {@link StreamConsumer}.
     *
     * @param outConsumer
     *            consumes the process's output stream.
     */
    public void setOutputConsumer(StreamConsumer outConsumer) {
        output = outConsumer;
    }

    /**
     * Executes the command and wait for the process to finish.
     *
     * @throws CruiseControlException
     *             for non-zero exit codes
     */
    public void executeAndWait() throws CruiseControlException {
        LOG.debug("Executing " + command.getExecutable());

        final int exitValue;
        try {
            exitValue = Processes.waitFor(command.execute(), output, error);
        } catch (Exception e) {
            throw new CruiseControlException(command.getExecutable() + " failed with exception", e);
        }

        if (exitValue != 0) {
            throw new CruiseControlException(command.getExecutable() + " process exited with error code " + exitValue);
        }
    }
}
