package cz.ctu.fee.dsv.semework;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class NodeCLI {

    private static Node currentNode = null;
    private static long currentNodeId = -1;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Interactive CLI");
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

                    case "leave":
                        leave();
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

                    case "kill":
                        killNode();
                        break;

                    case "revive":
                        reviveNode();
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

                    case "detect":
                        detectDeadNodes();
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
                e.printStackTrace();
            }
        }
    }

    private static void killNode() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }
        try {
            System.out.println("KILLING NODE " + currentNodeId + " (Simulating Failure)...");
            currentNode.kill();
            System.out.println("Node is now 'dead'.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void reviveNode() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }
        try {
            System.out.println("REVIVING NODE " + currentNodeId + "...");
            currentNode.revive();
            System.out.println("Node is back online.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void connect(String hostname, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            String registryName = String.valueOf(port);
            currentNode = (Node) registry.lookup(registryName);
            currentNodeId = currentNode.getNodeId();

            System.out.println("Connected to node ID: " + currentNodeId);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            currentNode = null;
            currentNodeId = -1;
        }
    }

    private static void addNode(String hostname, int port) {
        if (currentNode == null) {
            System.out.println("Not connected to any node. Use 'connect' first.");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            String registryName = String.valueOf(port);
            Node networkNode = (Node) registry.lookup(registryName);
            long networkNodeId = networkNode.getNodeId();

            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Joining node " + currentNodeId + " to network via node " + networkNodeId);
            System.out.println("═══════════════════════════════════════════════");

            java.util.List<Long> currentNodeTopology = currentNode.getKnownNodes();
            if (!currentNodeTopology.isEmpty()) {
                System.out.println("ERROR: Current node " + currentNodeId + " already has " +
                        currentNodeTopology.size() + " connections!");
                System.out.println("You can only add nodes FROM an isolated node TO a network node.");
                System.out.println("Current node topology:");
                for (Long id : currentNodeTopology) {
                    System.out.println("  - Node " + id);
                }
                System.out.println("\nCorrect usage:");
                System.out.println("  1. Connect to the NEW/ISOLATED node");
                System.out.println("  2. Run: addnode <existing-network-node-host> <port>");
                return;
            }

            System.out.println("Current node " + currentNodeId + " is isolated (0 connections)");
            System.out.println("Joining network through node " + networkNodeId);

            System.out.println("\nStep 1: Calling join() on network node " + networkNodeId);
            java.util.Map<Long, Node> existingNodes = networkNode.join(currentNodeId, currentNode);

            System.out.println("\nStep 2: Received " + existingNodes.size() + " existing nodes from network");
            for (Long id : existingNodes.keySet()) {
                System.out.println("  - Node " + id);
            }

            System.out.println("\nStep 3: Adding all existing nodes to node " + currentNodeId + "'s topology");
            for (java.util.Map.Entry<Long, Node> entry : existingNodes.entrySet()) {
                System.out.println("  - Adding node " + entry.getKey());
                currentNode.addNode(entry.getKey(), entry.getValue());
            }

            System.out.println("\n═══════════════════════════════════════════════");
            System.out.println("  Join complete! Complete graph established.");
            System.out.println("  Node " + currentNodeId + " now knows " + existingNodes.size() + " other nodes");
            System.out.println("  Total nodes in network: " + (existingNodes.size() + 1));
            System.out.println("═══════════════════════════════════════════════");
        } catch (Exception e) {
            System.err.println("Failed to add node: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void leave() {
        if (currentNode == null) {
            System.out.println("Not connected to any node. Use 'connect' first.");
            return;
        }

        try {
            java.util.List<Long> topologyBefore = currentNode.getKnownNodes();

            if (topologyBefore.isEmpty()) {
                System.out.println("═══════════════════════════════════════════════");
                System.out.println("Node " + currentNodeId + " is already isolated");
                System.out.println("═══════════════════════════════════════════════");
                return;
            }

            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Node " + currentNodeId + " is leaving the network");
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Current topology size: " + topologyBefore.size() + " nodes");
            System.out.println("Connected to nodes:");
            for (Long id : topologyBefore) {
                System.out.println("  - Node " + id);
            }

            System.out.println("\nExecuting leave operation...");
            currentNode.leave();

            java.util.List<Long> topologyAfter = currentNode.getKnownNodes();

            System.out.println("\n═══════════════════════════════════════════════");
            System.out.println("Leave operation complete!");
            System.out.println("  Topology before: " + topologyBefore.size() + " nodes");
            System.out.println("  Topology after: " + topologyAfter.size() + " nodes");

            if (topologyAfter.isEmpty()) {
                System.out.println("Node " + currentNodeId + " is now isolated");
            } else {
                System.out.println("Warning: Node still has " + topologyAfter.size() + " connections");
            }
            System.out.println("═══════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("Failed to leave network: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void listNodes() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
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
            System.out.println("Total: " + nodes.size() + " nodes");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void showStatus() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }

        try {
            System.out.println("Node Status:");
            System.out.println("  Node ID: " + currentNodeId);
            System.out.println("  In Critical Section: " + currentNode.isInCriticalSection());
            System.out.println("  Queue: " + currentNode.getQueueStatus());
            System.out.println("  Message Delay: " + currentNode.getMessageDelayMs() + "ms");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void showClock() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }

        try {
            int clock = currentNode.getLogicalClock();
            System.out.println("Logical Clock: " + clock);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void getVariable() {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }

        try {
            int value = currentNode.getSharedVariable();
            System.out.println("Shared Variable = " + value);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void setVariable(int value) {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }

        try {
            currentNode.setSharedVariable(value);
            System.out.println("Shared Variable set to " + value);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void setDelay(int delayMs) {
        if (currentNode == null) {
            System.out.println("Not connected to any node.");
            return;
        }

        try {
            currentNode.setMessageDelayMs(delayMs);
            System.out.println("Message delay set to " + delayMs + "ms");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void detectDeadNodes() {
        if (currentNode == null) {
            System.out.println("Not connected to any node. Use 'connect' first.");
            return;
        }

        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Starting failure detection on node " + currentNodeId);
            System.out.println("═══════════════════════════════════════════════");

            currentNode.detectDeadNodes();

            System.out.println("\n═══════════════════════════════════════════════");
            System.out.println("Failure detection complete");
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Check the node's console/log for detailed results.");

        } catch (Exception e) {
            System.err.println("Detection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void requestCS() {
        if (currentNode == null) {
            System.out.println("Not connected.");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("Requesting Critical Section (Background)...");

                currentNode.enterCS();

                System.out.println("\n[Async Notification] Entered Critical Section!");
                System.out.print("> ");
            } catch (Exception e) {
                System.err.println("\n[Async Notification] Request failed: " + e.getMessage());
                System.out.print("> ");
            }
        }).start();
    }

    private static void releaseCS() {
        if (currentNode == null) {
            System.out.println("Not connected.");
            return;
        }
        try {
            currentNode.leaveCS();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  connect <host> <port>         - Connect to a node");
        System.out.println("  addnode <host> <port>         - Join network (connect to NEW node first!)");
        System.out.println("  leave                         - Leave network gracefully");
        System.out.println("  list                          - List all known nodes");
        System.out.println("  status                        - Show node status");
        System.out.println("  clock                         - Show logical clock");
        System.out.println("  getvar                        - Get shared variable value");
        System.out.println("  setvar <value>                - Set shared variable value");
        System.out.println("  delay <ms>                    - Set message delay");
        System.out.println("  detect                        - Detect and remove dead nodes");
        System.out.println("  request                       - Request critical section");
        System.out.println("  release                       - Release critical section");
        System.out.println("  kill                          - Simulate node failure");
        System.out.println("  revive                        - Restore node from failure");
        System.out.println("  help                          - Show this help");
        System.out.println("  exit                          - Exit client");
        System.out.println();
    }
}