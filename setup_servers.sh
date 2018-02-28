################################################################################
# This script will launch the command cmd over ssh on all the machines listed in
# the list of machines.
#
# There is a script that will contain the commands that are to be run on each
# computer.
################################################################################


username=phcarb

machines="l4712-01 l4712-02 l4712-03"
server=info.polymtl.ca

cmd=~/Documents/GitHub/INF8480_TP2/setup_me.sh

for m in $machines ; do
	ssh $username@$m.$server $cmd &
done
