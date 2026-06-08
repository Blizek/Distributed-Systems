# Systemy rozproszone zadanie 7

### 1. Budowanie projektu
```bash
mvn clean package
```

### 2. Uruchomienie klastra ZooKeeper
```bash
chmod +x start-zk.sh stop-zk.sh
./start-zk.sh
```

### 3. Uruchomienie aplikacji
```bash
# macOS
java -jar target/zookeeper-watcher-1.0-SNAPSHOT.jar localhost:2181,localhost:2182,localhost:2183 open -a Calculator

# Linux
java -jar target/zookeeper-watcher-1.0-SNAPSHOT.jar localhost:2181,localhost:2182,localhost:2183 gnome-calculator

# Windows
java -jar target/zookeeper-watcher-1.0-SNAPSHOT.jar localhost:2181,localhost:2182,localhost:2183 calc.exe
```

### 4. Komendy w konsoli aplikacji
* `tree`
* `quit`

### 5. Uruchomienie Zookeeper CLI
```bash
# macOS / Linux
./apache-zookeeper-3.8.4-bin/bin/zkCli.sh -server localhost:2181,localhost:2182,localhost:2183

# Windows
apache-zookeeper-3.8.4-bin\bin\zkCli.cmd -server localhost:2181,localhost:2182,localhost:2183
```

### 6. Zatrzymanie klastra ZooKeeper
```bash
./stop-zk.sh
```
