package cz.ctu.fee.dsv.semework;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class NodeCLI {
    private static Node currentNode = null;
    private static long currentNodeId = -1;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Interactive Node CLI");
        printHelp();

        while (true) {
            System.out.print(getPrompt());
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

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
                        System.out.println("Unknown command: " + command + ". Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static String getPrompt() {
        return currentNode == null ? "[Not Connected]> " : "[Node " + currentNodeId + "]> ";
    }

    private static void killNode() {
        if (currentNode == null) { System.out.println("Not connected to any node."); return; }
        try {
            System.out.println("KILLING NODE " + currentNodeId);
            currentNode.kill();
            System.out.println("Node is now 'dead'");
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void reviveNode() {
        if (currentNode == null) { System.out.println("Not connected to any node."); return; }
        try {
            System.out.println("REVIVING NODE " + currentNodeId);
            currentNode.revive();
            System.out.println("Node is back online");
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void connect(String hostname, int port) {
        try {
            System.out.println("Connecting to " + hostname + ":" + port);
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            currentNode = (Node) registry.lookup(String.valueOf(port));
            currentNodeId = currentNode.getNodeId();
            System.out.println("Connected to node ID: " + currentNodeId);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            currentNode = null;
            currentNodeId = -1;
        }
    }

    private static void addNode(String hostname, int port) {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            Node networkNode = (Node) registry.lookup(String.valueOf(port));
            long networkNodeId = networkNode.getNodeId();

            System.out.println("Joining network. Current node: " + currentNodeId + ", Network node: " + networkNodeId);
            if (!currentNode.getKnownNodes().isEmpty()) {
                System.out.println("ERROR: Current node already has connections. Must be isolated to join network.");
                return;
            }

            java.util.Map<Long, Node> existingNodes = networkNode.join(currentNodeId, currentNode);
            for (java.util.Map.Entry<Long, Node> entry : existingNodes.entrySet()) {
                currentNode.addNode(entry.getKey(), entry.getValue());
            }

            System.out.println("Join complete! Known nodes: " + existingNodes.size());
        } catch (Exception e) { System.err.println("Failed to add node: " + e.getMessage()); }
    }

    private static void leave() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            java.util.List<Long> topologyBefore = currentNode.getKnownNodes();
            if (topologyBefore.isEmpty()) { System.out.println("Node " + currentNodeId + " is isolated."); return; }

            System.out.println("Leaving network. Node: " + currentNodeId);
            currentNode.leave();
            java.util.List<Long> topologyAfter = currentNode.getKnownNodes();
            System.out.println("Leave complete. Remaining connections: " + topologyAfter.size());
        } catch (Exception e) { System.err.println("Failed to leave network: " + e.getMessage()); }
    }

    private static void listNodes() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            java.util.List<Long> nodes = currentNode.getKnownNodes();
            System.out.println("Known Nodes:");
            if (nodes.isEmpty()) System.out.println("(none)");
            else nodes.forEach(id -> System.out.println("Node ID: " + id));
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void showStatus() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            System.out.println("Node Status:");
            System.out.println("Node ID: " + currentNodeId);
            System.out.println("Logical Clock: " + currentNode.getLogicalClock());
            System.out.println("In CS: " + currentNode.isInCriticalSection());
            System.out.println("Request Queue: " + currentNode.getQueueStatus());
            System.out.println("Message Delay: " + currentNode.getMessageDelayMs() + "ms");
            System.out.println("Known Nodes: " + currentNode.getKnownNodes().size());
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void showClock() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try { System.out.println("Logical Clock: " + currentNode.getLogicalClock()); }
        catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void getVariable() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try { System.out.println("Shared Variable = " + currentNode.getSharedVariable()); }
        catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void setVariable(int value) {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            currentNode.setSharedVariable(value);
            System.out.println("Shared Variable set to " + value);
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void setDelay(int delayMs) {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            currentNode.setMessageDelayMs(delayMs);
            System.out.println("Message delay set to " + delayMs + "ms");
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void detectDeadNodes() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            System.out.println("Starting failure detection...");
            currentNode.detectDeadNodes();
            System.out.println("Failure detection complete. Check node logs for details.");
        } catch (Exception e) { System.err.println("Detection failed: " + e.getMessage()); }
    }

    private static void requestCS() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        System.out.println("Requesting critical section (async)");
        new Thread(() -> {
            try {
                currentNode.enterCS();
                System.out.println("CRITICAL SECTION ENTERED");
                System.out.print(getPrompt());
            } catch (Exception e) {
                System.err.println("CRITICAL SECTION REQUEST FAILED: " + e.getMessage());
                System.out.print(getPrompt());
            }
        }).start();
    }

    private static void releaseCS() {
        if (currentNode == null) { System.out.println("Not connected."); return; }
        try {
            currentNode.leaveCS();
            System.out.println("Critical section released");
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void printHelp() {
        System.out.println("Available Commands");
        System.out.println("connect <host> <port>");
        System.out.println("addnode <host> <port>");
        System.out.println("leave");
        System.out.println("list");
        System.out.println("request");
        System.out.println("release");
        System.out.println("getvar");
        System.out.println("setvar <value>");
        System.out.println("status");
        System.out.println("clock");
        System.out.println("delay <ms>");
        System.out.println("kill");
        System.out.println("revive");
        System.out.println("detect");
        System.out.println("help");
        System.out.println("exit");
    }
}
