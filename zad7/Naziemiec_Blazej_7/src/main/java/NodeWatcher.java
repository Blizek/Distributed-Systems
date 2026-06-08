import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class NodeWatcher implements Watcher {

    private static final String WATCHED_NODE = "/a";

    private final ZooKeeper zk;
    private final ExternalAppManager appManager;

    public NodeWatcher(ZooKeeper zk, ExternalAppManager appManager) {
        this.zk = zk;
        this.appManager = appManager;
    }

    public void startWatching() {
        try {
            Stat stat = zk.exists(WATCHED_NODE, this);
            if (stat != null) {
                System.out.println("[Watcher] Znode /a already exists.");
                appManager.startApp();
                watchDescendants();
            } else {
                System.out.println("[Watcher] Znode /a does not exist yet. Waiting...");
            }
        } catch (KeeperException | InterruptedException e) {
            System.err.println("[Watcher] Error checking /a existence: " + e.getMessage());
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.None) {
            return;
        }

        String path = event.getPath();

        switch (event.getType()) {
            case NodeCreated:
                if (WATCHED_NODE.equals(path)) {
                    System.out.println("[Watcher] Znode /a has been CREATED.");
                    appManager.startApp();
                    watchDescendants();
                    reRegisterExistsWatch();
                }
                break;

            case NodeDeleted:
                if (WATCHED_NODE.equals(path)) {
                    System.out.println("[Watcher] Znode /a has been DELETED.");
                    appManager.stopApp();
                    reRegisterExistsWatch();
                }
                break;

            case NodeChildrenChanged:
                if (path != null && (path.equals(WATCHED_NODE) || path.startsWith(WATCHED_NODE + "/"))) {
                    watchDescendants();
                }
                break;

            default:
                reRegisterExistsWatch();
                break;
        }
    }

    private void reRegisterExistsWatch() {
        try {
            zk.exists(WATCHED_NODE, this);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("[Watcher] Error re-registering exists watch: " + e.getMessage());
        }
    }

    private void watchDescendants() {
        try {
            Stat stat = zk.exists(WATCHED_NODE, false);
            if (stat == null) {
                return;
            }
            int count = countAndWatchRecursive(WATCHED_NODE);
            System.out.println("  Znode /a – total number of descendants: " + count);
            showDescendantCountDialog(count);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("[Watcher] Error counting descendants of /a: " + e.getMessage());
        }
    }

    private int countAndWatchRecursive(String path) throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(path, this);
        int count = children.size();
        for (String child : children) {
            String childPath = path + "/" + child;
            count += countAndWatchRecursive(childPath);
        }
        return count;
    }

    private void showDescendantCountDialog(int count) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Total number of descendants of /a: " + count,
                    "ZooKeeper – Descendants of /a",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    public void printTree() {
        try {
            Stat stat = zk.exists(WATCHED_NODE, false);
            if (stat == null) {
                System.out.println("[Tree] Znode /a does not exist.");
                return;
            }
            System.out.println("[Tree] Structure of /a:");
            printTreeRecursive(WATCHED_NODE, 0);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("[Tree] Error printing tree: " + e.getMessage());
        }
    }

    private void printTreeRecursive(String path, int depth) throws KeeperException, InterruptedException {
        String indent = "  ".repeat(depth);
        String nodeName = path.equals(WATCHED_NODE) ? "/a" : path.substring(path.lastIndexOf('/') + 1);
        System.out.println(indent + "├── " + nodeName);

        List<String> children = zk.getChildren(path, false);
        for (String child : children) {
            String childPath = path + "/" + child;
            printTreeRecursive(childPath, depth + 1);
        }
    }
}
