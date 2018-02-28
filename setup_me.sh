log_info() {
	echo "$(date) : $1" | tee -a ~/Documents/GitHub/INF8480_TP2/info.lst
}

pushd ~/Documents/GitHub/INF8480_TP2/

if ! [ -d bin ] ; then
	ant
fi

pushd ./bin
rmiregistry &
log_info "Launched rmi_registry in $(pwd) on $(hostname)"
popd
sleep 1
# ./server &
log_info "Launched server in $(pwd) on $(hostname)"
sleep 30

popd
