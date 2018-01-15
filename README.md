easy-manage-deposit
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-manage-deposit.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-manage-deposit)


SYNOPSIS
--------
   
     easy-manage-deposit full [<depositor>]
     easy-manage-deposit summary [<depositor>]
     
ARGUMENTS
--------
   
     Options:
   
     
         --help      Show help message
         --version   Show version of this program
   
     Subcommand: full
         --help   Show help message
         
      trailing arguments:
       depositor (not required)
     ---
   
     Subcommand: summary
         --help   Show help message
         
      trailing arguments:
       depositor (not required) 
     ---
    
     
DESCRIPTION
-----------

Creates report about the deposits in the deposit area.
     
EXAMPLES
--------

     easy-manage-deposit full mendeleydata
     easy-manage-deposit summary mendeleydata
     easy-manage-deposit full dryad
     easy-manage-deposit summary dryad


INSTALLATION AND CONFIGURATION
------------------------------


1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-manage-deposit-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-manage-deposit-<version>/bin/easy-manage-deposit /usr/bin



General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-manage-deposit.git
        cd easy-manage-deposit
        mvn install
