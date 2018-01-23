easy-manage-deposit
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-manage-deposit.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-manage-deposit)


SYNOPSIS
--------
   
     easy-manage-deposit report full [<depositor>]
     easy-manage-deposit report summary [<depositor>]
     easy-manage-deposit clean [<depositor>]
     
         
ARGUMENTS
--------
   
     Options:
   
     
         --help      Show help message
         --version   Show version of this program
   
       Subcommand: report
             --help   Show help message
     
       Subcommand: report full
             --help   Show help message
     
        trailing arguments:
         depositor (not required)
       ---
     
       Subcommand: report summary
             --help   Show help message
     
        trailing arguments:
         depositor (not required)
       ---
       Subcommand: clean
             --help   Show help message
     
        trailing arguments:
         depositor (not required)
     ---
    
     
DESCRIPTION
-----------

Manages the deposits in the deposit area.
     
EXAMPLES
--------

     easy-manage-deposit report full < UserId of the depositor >
     easy-manage-deposit report summary < UserId of the depositor >
     easy-manage-deposit report full < UserId of the depositor >
     easy-manage-deposit report summary < UserId of the depositor >
     easy-manage-deposit clean  < UserId of the depositor >
     easy-manage-deposit clean  < UserId of the depositor >
     


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
