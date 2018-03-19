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

send-keys -t 5 'sleep 2 ; ssh pi@rpi "cd Documents/GitHub/INF8480_TP2 ; ./prepare_distant_rpi"' enter

## Just a convenience shell
select-pane -t 1

attach