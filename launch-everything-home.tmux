split-window -d -h
split-window -d -t 1
split-window -d -t 1
split-window -d -t 1
split-window -d -t 0
split-window -d -t 5


send-keys -t 0 'CLASSPATH=bin rmiregistry' enter
send-keys -t 2 'sleep 1 ; ./LDAP asdf' enter

send-keys -t 3 'sleep 2 ; ./dispatcher' enter

send-keys -t 4 'sleep 2 ; ./server 0 20 localhost' enter

send-keys -t 5 'ssh pi@rpi "cd Documents/GitHub/INF8480_TP2 ; ./prepare_distant_rpi"' enter

# Leave the user in pane number 1 so they may enter commands.
select-pane -t 1
