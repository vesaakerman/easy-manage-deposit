easy-deposit-report
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-deposit-report.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-deposit-report)

<!-- Remove this comment and extend the descriptions below -->


SYNOPSIS
--------

    easy-deposit-report (synopsis of command line parameters)
    easy-deposit-report (... possibly multiple lines for subcommands)


DESCRIPTION
-----------

Creates report about the deposits in the deposit area.


ARGUMENTS
---------

    Options:

        --help      Show help message
        --version   Show version of this program

    Subcommand: run-service - Starts EASY Deposit Report as a daemon that services HTTP requests
        --help   Show help message
    ---

EXAMPLES
--------

    easy-deposit-report -o value


INSTALLATION AND CONFIGURATION
------------------------------


1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-deposit-report-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-deposit-report-<version>/bin/easy-deposit-report /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-deposit-report.git
        cd easy-deposit-report
        mvn install
