#!/bin/bash

################################################################################
# CruiseControl, a Continuous Integration Toolkit
# Copyright (c) 2001, ThoughtWorks, Inc.
# 651 W Washington Ave. Suite 500
# Chicago, IL 60661 USA
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#     + Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#
#     + Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
#
#     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
#       names of its contributors may be used to endorse or promote
#       products derived from this software without specific prior
#       written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
################################################################################

# The root of the CruiseControl directory.  The key requirement is that this is the parent
# directory of CruiseControl's lib and dist directories.
# By default assume they are using the batch file from the local directory.
CCDIR=..
LIBDIR=$CCDIR/lib
DISTDIR=$CCDIR/dist

CRUISE_PATH=$JAVA_HOME/lib/tools.jar:$DISTDIR/cruisecontrol.jar:$LIBDIR/log4j.jar:$LIBDIR/jdom.jar:$LIBDIR/ant.jar:$LIBDIR/xerces.jar:$LIBDIR/mail.jar:$LIBDIR/optional.jar:$LIBDIR/junit.jar:$LIBDIR/activation.jar:.$/LIBDIR/starteam-sdk-interfaceonly.jar

EXEC="java -cp $CRUISE_PATH CruiseControl $@"
echo $EXEC
$EXEC

