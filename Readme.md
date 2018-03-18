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

To prepare for testing, you need to have two terminal windows open.  In one
window, call the prepare_local script.

In the second, call
	ssh pi@rpi 'cd Documents/GitHub/INF8480_TP2 ; ./prepare_distant_rpi

Scripts
-------

prepare_distant_rmi : Starts rmiregistry and a server on a distant machine

prepare_local : Stars rmiregistry, starts LDAP, starts a dispatcher and starts a
server all on the local machine.

To keep the processes going, both scripts end with a read.  They can be
terminated by pressing ENTER.

Test
----

In another terminal window and run test.sh.

Our local terminal will show output from LDAP, dispatcher and server.

Our remote terminal will show output from Server on the remote machine.

