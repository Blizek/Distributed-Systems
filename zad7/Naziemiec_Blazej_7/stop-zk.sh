#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

ZK_BIN=$(find "$SCRIPT_DIR" -type f -path "*/bin/zkServer.sh" | head -n 1)

if [ -z "$ZK_BIN" ]; then
    echo "ERROR: Nie znaleziono skryptu zkServer.sh."
    echo "Upewnij się, że skopiowałeś rozpakowany folder Apache ZooKeeper bezpośrednio do tego folderu (Naziemiec_Blazej_7)."
    exit 1
fi

ZOOKEEPER_HOME=$(dirname "$(dirname "$ZK_BIN")")

ZK_SERVER="$ZOOKEEPER_HOME/bin/zkServer.sh"

echo "Stopping ZooKeeper servers..."

for i in 1 2 3; do
    CONF="$SCRIPT_DIR/conf/zoo$i.cfg"
    "$ZK_SERVER" stop "$CONF"
done

echo "All servers stopped."
