package client;

import compute.Node;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Interactive client for testing the distributed system
 *
 * Usage: java client.Client
 */
public class Client {

    private static Node currentNode = null;
    private static String currentNodeId = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("Distributed System - Interactive Client");
        System.out.println("=================================================");
        System.out.println();

        printHelp();

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "connect":
                        if (parts.length < 4) {
                            System.out.println("Usage: connect <hostname> <port> <nodeName>");
                            break;
                        }
                        connect(parts[1], Integer.parseInt(parts[2]), parts[3]);
                        break;

                    case "addnode":
                        if (parts.length < 4) {
                            System.out.println("Usage: addnode <hostname> <port> <nodeName>");
                            break;
                        }
                        addNode(parts[1], Integer.parseInt(parts[2]), parts[3]);
                        break;

                    case "list":
                        listNodes();
                        break;

                    case "status":
                        showStatus();
                        break;

                    case "clock":
                        showClock();
                        break;

                    case "getvar":
                        getVariable();
                        break;

                    case "setvar":
                        if (parts.length < 2) {
                            System.out.println("Usage: setvar <value>");
                            break;
                        }
                        setVariable(Integer.parseInt(parts[1]));
                        break;

                    case "delay":
                        if (parts.length < 2) {
                            System.out.println("Usage: delay <milliseconds>");
                            break;
                        }
                        setDelay(Integer.parseInt(parts[1]));
                        break;

                    case "ping":
                        ping();
                        break;

                    case "request":
                        requestCS();
                        break;

                    case "release":
                        releaseCS();
                        break;

                    case "help":
                        printHelp();
                        break;

                    case "exit":
                    case "quit":
                        System.out.println("Goodbye!");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void connect(String hostname, int port, String nodeName) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            currentNode = (Node) registry.lookup(nodeName);
            currentNodeId = currentNode.getNodeId();
            System.out.println("✓ Connected to node: " + currentNodeId);
        } catch (Exception e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            currentNode = null;
            currentNodeId = null;
        }
    }

    private static void addNode(String hostname, int port, String nodeName) {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node. Use 'connect' first.");
            return;
        }

        try {
            // Lookup the node to add
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            Node nodeToAdd = (Node) registry.lookup(nodeName);
            String nodeToAddId = nodeToAdd.getNodeId();

            // Add to current node's topology
            currentNode.addNode(nodeToAddId, nodeToAdd);

            // Also add current node to the new node's topology (bidirectional)
            nodeToAdd.addNode(currentNodeId, currentNode);

            System.out.println("✓ Node added to topology: " + nodeToAddId);
            System.out.println("  (Bidirectional connection established)");
        } catch (Exception e) {
            System.err.println("✗ Failed to add node: " + e.getMessage());
        }
    }

    private static void listNodes() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            System.out.println("Known nodes in topology:");
            java.util.List<String> nodes = currentNode.getKnownNodes();
            if (nodes.isEmpty()) {
                System.out.println("  (no other nodes)");
            } else {
                for (String nodeId : nodes) {
                    System.out.println("  - " + nodeId);
                }
            }
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void showStatus() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            System.out.println("Node Status:");
            System.out.println("  Node ID: " + currentNodeId);
            System.out.println("  In Critical Section: " + currentNode.isInCriticalSection());
            System.out.println("  Queue: " + currentNode.getQueueStatus());
            System.out.println("  Message Delay: " + currentNode.getMessageDelayMs() + "ms");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void showClock() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            int clock = currentNode.getLogicalClock();
            System.out.println("Logical Clock: " + clock);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void getVariable() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            int value = currentNode.getSharedVariable();
            System.out.println("Shared Variable = " + value);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void setVariable(int value) {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            currentNode.setSharedVariable(value);
            System.out.println("✓ Shared Variable set to " + value);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void setDelay(int delayMs) {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            currentNode.setMessageDelayMs(delayMs);
            System.out.println("✓ Message delay set to " + delayMs + "ms");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void ping() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            currentNode.ping();
            System.out.println("✓ Ping successful");
        } catch (Exception e) {
            System.err.println("✗ Ping failed: " + e.getMessage());
        }
    }

    private static void requestCS() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            // This is a placeholder - real implementation will be added with Lamport algorithm
            System.out.println("Request CS (not yet implemented - Lamport algorithm pending)");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void releaseCS() {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node.");
            return;
        }

        try {
            // This is a placeholder - real implementation will be added with Lamport algorithm
            System.out.println("Release CS (not yet implemented - Lamport algorithm pending)");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  connect <host> <port> <name>  - Connect to a node");
        System.out.println("  addnode <host> <port> <name>  - Add a node to topology");
        System.out.println("  list                          - List all known nodes");
        System.out.println("  status                        - Show node status");
        System.out.println("  clock                         - Show logical clock");
        System.out.println("  getvar                        - Get shared variable value");
        System.out.println("  setvar <value>                - Set shared variable value");
        System.out.println("  delay <ms>                    - Set message delay");
        System.out.println("  ping                          - Ping the node");
        System.out.println("  request                       - Request critical section (TODO)");
        System.out.println("  release                       - Release critical section (TODO)");
        System.out.println("  help                          - Show this help");
        System.out.println("  exit                          - Exit client");
    }
}