package ctu.dsv.semwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ConsoleHandler implements Runnable {

    private boolean reading = true;
    private BufferedReader reader = null;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private Node currentNode;
    private long currentNodeId = -1;

    public ConsoleHandler(NodeImpl myNode) {
        this.currentNode = myNode;
        try {
            this.currentNodeId = myNode.getNodeId();
        } catch (Exception e) {
            err.println("Error getting node ID: " + e.getMessage());
        }
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    private String getPrompt() {
        return "[Node " + currentNodeId + "]> ";
    }

    private void parse_commandline(String commandline) {
        if (commandline.trim().isEmpty()) return;

        String[] parts = commandline.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "connect":
                    if (parts.length < 3) {
                        out.println("Usage: connect <hostname> <port>");
                        break;
                    }
                    connect(parts[1], Integer.parseInt(parts[2]));
                    break;
                case "addnode":
                    if (parts.length < 3) {
                        out.println("Usage: addnode <hostname> <port>");
                        break;
                    }
                    addNode(parts[1], Integer.parseInt(parts[2]));
                    break;
                case "leave":
                    currentNode.leave();
                    break;
                case "list":
                    currentNode.getKnownNodes().forEach(id -> out.println("Node ID: " + id));
                    break;
                case "status":
                    showStatus();
                    break;
                case "clock":
                    currentNode.getLogicalClock();
                    break;
                case "kill":
                    currentNode.kill();
                    break;
                case "revive":
                    currentNode.revive();
                    break;
                case "getvar":
                    currentNode.getSharedVariable();
                    break;
                case "setvar":
                    if (parts.length < 2) {
                        out.println("Usage: setvar <value>");
                        break;
                    }
                    currentNode.setSharedVariable(Integer.parseInt(parts[1]));
                    break;
                case "delay":
                    if (parts.length < 2) {
                        out.println("Usage: delay <milliseconds>");
                        break;
                    }
                    currentNode.setMessageDelayMs(Integer.parseInt(parts[1]));
                    break;
                case "detect":
                    currentNode.detectDeadNodes();
                    break;
                case "request":
                    requestCS();
                    break;
                case "release":
                    currentNode.leaveCS();
                    break;
                case "queue":
                    currentNode.getQueueStatus();
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    out.println("Goodbye!");
                    reading = false;
                    System.exit(0);
                    break;
                default:
                    out.println("Unrecognized command. Type 'help' for available commands");
            }
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
        }
    }

    private void connect(String hostname, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            currentNode = (Node) registry.lookup(String.valueOf(port));
            currentNodeId = currentNode.getNodeId();
            out.println("Connected to node ID: " + currentNodeId);
        } catch (Exception e) {
            err.println("Connection failed: " + e.getMessage());
            currentNode = null;
            currentNodeId = -1;
        }
    }

    private void addNode(String hostname, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            Node networkNode = (Node) registry.lookup(String.valueOf(port));

            java.util.Map<Long, Node> existingNodes = networkNode.join(currentNodeId, currentNode);
            for (java.util.Map.Entry<Long, Node> entry : existingNodes.entrySet()) {
                currentNode.addNode(entry.getKey(), entry.getValue());
            }

        } catch (Exception e) { err.println("Failed to add node: " + e.getMessage()); }
    }

    private void showStatus() {
        try {
            out.println("Node Status:");
            out.println("Node ID: " + currentNodeId);
            out.println("Logical Clock: " + currentNode.getLogicalClock());
            out.println("In CS: " + currentNode.isInCriticalSection());
            out.println("Request Queue: " + currentNode.getQueueStatus());
            out.println("Message Delay: " + currentNode.getMessageDelayMs() + "ms");
            out.println("Known Nodes: " + currentNode.getKnownNodes().size());
        } catch (Exception e) { err.println("Error: " + e.getMessage()); }
    }

    private void requestCS() {
        out.println("Requesting critical section (async)");
        new Thread(() -> {
            try {
                currentNode.enterCS();
                out.print(getPrompt());
            } catch (Exception e) {
                err.println("CRITICAL SECTION REQUEST FAILED: " + e.getMessage());
                out.print(getPrompt());
            }
        }).start();
    }

    private void printHelp() {
        out.println("Available Commands");
        out.println("connect <host> <port> - Connect to remote node");
        out.println("addnode <host> <port> - Join network via node");
        out.println("leave                 - Leave network");
        out.println("list                  - List known nodes");
        out.println("request               - Request critical section");
        out.println("release               - Release critical section");
        out.println("getvar                - Get shared variable");
        out.println("setvar <value>        - Set shared variable");
        out.println("status                - Show node status");
        out.println("clock                 - Show logical clock");
        out.println("queue                 - Show request queue");
        out.println("delay <ms>            - Set message delay");
        out.println("kill                  - Simulate node crash");
        out.println("revive                - Revive crashed node");
        out.println("detect                - Detect dead nodes");
        out.println("help                  - Show this help");
        out.println("exit                  - Exit program");
    }

    @Override
    public void run() {
        String commandline = "";
        printHelp();

        while (reading == true) {
            commandline = "";
            out.print(getPrompt());
            try {
                commandline = reader.readLine();
                parse_commandline(commandline);
            } catch (IOException e) {
                err.println("ConsoleHandler - error in reading console input.");
                e.printStackTrace();
                reading = false;
            }
        }
        out.println("Closing ConsoleHandler.");
    }

    public void stop() {
        reading = false;
    }
}