easy-manage-deposit
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-manage-deposit.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-manage-deposit)


SYNOPSIS
--------
   
    easy-manage-deposit report full [-a, --age <n>] [<depositor>]
    easy-manage-deposit report summary [-a, --age <n>] [<depositor>]
    easy-manage-deposit report error [-a, --age <n>] [<depositor>]
    easy-manage-deposit clean [-d, --data-only] [-s, --state <state>] [-k, --keep <n>] [-l, --new-state-label <state>] [-n, --new-state-description <description>] [-f, --force] [-o, --output] [--do-update] [<depositor>]
    easy-manage-deposit sync-fedora-state <easy-dataset-id>

     
         
ARGUMENTS
--------
   
     Options:
            -h, --help      Show help message
            -v, --version   Show version of this program
          
          Subcommand: report
            -h, --help   Show help message
          
          Subcommand: report full - creates a full report for depositor(optional)
            -a, --age  <arg>   Only report on the deposits that are less than n days old.
                               An age argument of n=0 days corresponds to 0<=n<1. If this
                               argument is not provided, all deposits will be reported on.
            -h, --help         Show help message
          
           trailing arguments:
            depositor (not required)
          ---
          
          Subcommand: report summary - creates a summary report for depositor(optional)
            -a, --age  <arg>   Only report on the deposits that are less than n days old.
                               An age argument of n=0 days corresponds to 0<=n<1. If this
                               argument is not provided, all deposits will be reported on.
            -h, --help         Show help message
          
           trailing arguments:
            depositor (not required)
          ---
          
          Subcommand: report error - creates a report displaying all failed, rejected and invalid deposits for depositor(optional)
            -a, --age  <arg>   Only report on the deposits that are less than n days old.
                               An age argument of n=0 days corresponds to 0<=n<1. If this
                               argument is not provided, all deposits will be reported on.
            -h, --help         Show help message
          
           trailing arguments:
            depositor (not required)
          ---
          Subcommand: clean - removes deposit with specified state
            -d, --data-only                      If specified, the deposit.properties and
                                                 the container file of the deposit are not
                                                 deleted
                --do-update                      Do the actual deleting of deposits and
                                                 updating of deposit.properties
            -f, --force                          The user is not asked for a confirmation
            -k, --keep  <arg>                    The deposits whose ages are greater than
                                                 or equal to the argument n (days) are
                                                 deleted. An age argument of n=0 days
                                                 corresponds to 0<=n<1. (default = -1)
            -n, --new-state-description  <arg>   The state description in
                                                 deposit.properties after the deposit has
                                                 been deleted
            -l, --new-state-label  <arg>         The state label in deposit.properties
                                                 after the deposit has been deleted
            -o, --output                         Output a list of depositIds of the
                                                 deposits that were deleted
            -s, --state  <arg>                   The deposits with the specified state
                                                 argument are deleted
            -h, --help                           Show help message
            
           trailing arguments:
            depositor (not required)
          ---
          
          Subcommand: sync-fedora-state - Syncs a deposit with Fedora, checks if the deposit is properly registered in Fedora and updates the deposit.properties accordingly
            -h, --help   Show help message
          
           trailing arguments:
            easy-dataset-id (required)   The dataset identifier of the deposit which
                                         deposit.properties are being synced with Fedora
          ---
    
     
DESCRIPTION
-----------

Manages the deposits in the deposit area.
     
EXAMPLES
--------

     easy-manage-deposit report error someUserId
     easy-manage-deposit report full someUserId
     easy-manage-deposit report summary someUserId
     easy-manage-deposit report error -a 0 someUserId
     easy-manage-deposit report full -a 0 someUserId
     easy-manage-deposit report summary --age 2 someUserId
     easy-manage-deposit clean someUserId
     easy-manage-deposit clean --data-only --state <state> --keep <n> someUserId
     easy-manage-deposit retry someUserId


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
