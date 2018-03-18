Preparation
===========

To run the system, we need
* LDAP
* Dispatcher
* Server (maybe more than one)
to be running.

The LDAP and Dispatcher will be network objects running locally.  Also we will
have a server running on the local machine and one running on another machine
(and possibly more).

Each machine needs to have _rmiregistry_ running.

Scripts
-------

prepare_distant_rmi : Starts rmiregistry and a server on a distant machine

prepare_local : Stars rmiregistry, starts LDAP, starts a dispatcher and starts a
server all on the local machine.

Right now, prepare_local also calls prepare_distant_rpi on the distant machine
over ssh

To keep the processes going, both scripts end with a read.  We need to press
enter once to terminate the remote process, and once again to terminate the
local process.

Test
----

Once the preparation is done, the processes must keep running.  We need to open
another terminal window and run test.sh.

Our first terminal window will show the output of both the remote machine and
the local machine.


