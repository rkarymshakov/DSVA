package cz.ctu.fee.dsv.semework;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Interactive client for testing the distributed system with numeric node IDs
 *
 * Usage: java Client
 */
public class NodeCLI {

    private static Node currentNode = null;
    private static long currentNodeId = -1;  // Numeric ID (long)

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("Distributed System - Interactive Client (Numeric Node IDs)");
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
                        if (parts.length < 3) {
                            System.out.println("Usage: connect <hostname> <port>");
                            break;
                        }
                        connect(parts[1], Integer.parseInt(parts[2]));
                        break;

                    case "addnode":
                        if (parts.length < 3) {
                            System.out.println("Usage: addnode <hostname> <port>");
                            break;
                        }
                        addNode(parts[1], Integer.parseInt(parts[2]));
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
                        System.out.println("Sleep a bit. And then lock in and finish project");
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

    private static void connect(String hostname, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            String registryName = String.valueOf(port);  // RMI registration name is port
            currentNode = (Node) registry.lookup(registryName);
            currentNodeId = currentNode.getNodeId();  // Get numeric long ID
            System.out.println("✓ Connected to node ID: " + currentNodeId);
        } catch (Exception e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            currentNode = null;
            currentNodeId = -1;
        }
    }

    private static void addNode(String hostname, int port) {
        if (currentNode == null) {
            System.out.println("✗ Not connected to any node. Use 'connect' first.");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            String registryName = String.valueOf(port);
            Node nodeToAdd = (Node) registry.lookup(registryName);
            long nodeToAddId = nodeToAdd.getNodeId();  // Numeric ID

            // Add to current node's topology
            currentNode.addNode(nodeToAddId, nodeToAdd);

            // Bidirectional
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
            java.util.List<Long> nodes = currentNode.getKnownNodes();
            if (nodes.isEmpty()) {
                System.out.println("  (no other nodes)");
            } else {
                for (Long nodeId : nodes) {
                    System.out.println("  - Node ID: " + nodeId);
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
            // Placeholder - will be implemented later with Lamport
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
            // Placeholder
            System.out.println("Release CS (not yet implemented - Lamport algorithm pending)");
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  connect <host> <port>         - Connect to a node (port is registry name)");
        System.out.println("  addnode <host> <port>         - Add a node to topology");
        System.out.println("  list                          - List all known nodes (numeric IDs)");
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