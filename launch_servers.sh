machine_numbers="01 02 03"

machine_list=""

for n in $machine_numbers ; do
	machine_list="$machine_list l4712-$n"
done

ssh_command="ssh l4712-01.info.polymtl.ca"
