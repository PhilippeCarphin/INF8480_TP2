split-window -d -h
split-window -d -t 1
split-window -d -t 1
split-window -d -t 1
split-window -d -t 0
split-window -d -t 5


send-keys -t 0 'CLASSPATH=bin rmiregistry ; echo "rmiregistry running here"' enter
send-keys -t 2 'sleep 1 ; ./LDAP' enter

send-keys -t 3 'sleep 2 ; ./dispatcher' enter

send-keys -t 4 'sleep 2 ; ./server' enter

send-keys -t 5 'sleep 2 ; ./start-remote-server l4712-11.info.polymtl.ca l4712-10.info.polymtl.ca 0 8 phcarb' enter
send-keys -t 6 'sleep 2 ; ./start-remote-server l4712-12.info.polymtl.ca l4712-10.info.polymtl.ca 0 8 phcarb' enter

# Leave the user in pane number 1 so they may enter commands.
select-pane -t 1
