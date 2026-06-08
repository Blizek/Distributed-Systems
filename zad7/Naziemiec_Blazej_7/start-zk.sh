#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Znajdź lokalny folder zookeeper (np. apache-zookeeper-3.8.4-bin)
ZK_BIN=$(find "$SCRIPT_DIR" -type f -path "*/bin/zkServer.sh" | head -n 1)

if [ -z "$ZK_BIN" ]; then
    echo "ERROR: Nie znaleziono skryptu zkServer.sh."
    echo "Upewnij się, że skopiowałeś rozpakowany folder Apache ZooKeeper bezpośrednio do tego folderu (Naziemiec_Blazej_7)."
    exit 1
fi

ZOOKEEPER_HOME=$(dirname "$(dirname "$ZK_BIN")")

ZK_SERVER="$ZOOKEEPER_HOME/bin/zkServer.sh"

echo "Using ZOOKEEPER_HOME=$ZOOKEEPER_HOME"
echo "Using zkServer.sh at $ZK_SERVER"
echo

for i in 1 2 3; do
    DATA_DIR="$SCRIPT_DIR/data/zk$i"
    mkdir -p "$DATA_DIR"
    echo "$i" > "$DATA_DIR/myid"
    echo "Created data dir: $DATA_DIR (myid=$i)"
done

echo
echo "Starting ZooKeeper servers..."

for i in 1 2 3; do
    CONF="$SCRIPT_DIR/conf/zoo$i.cfg"
    echo "Starting server $i with config $CONF ..."
    "$ZK_SERVER" start "$CONF"
done

echo
echo "All 3 ZooKeeper servers started."
echo "Connect string: localhost:2181,localhost:2182,localhost:2183"
echo
echo "To stop all servers, run: ./stop-zk.sh"
