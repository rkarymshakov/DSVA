package ctu.dsv.semwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

public class ConsoleHandler implements Runnable {

    private boolean reading = true;
    private final BufferedReader reader;
    private final NodeImpl myNode;

    public ConsoleHandler(NodeImpl myNode) {
        this.myNode = myNode;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    private void parseCommandLine(String commandline) {
        String[] parts = commandline.trim().split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "status":
                case "s":
                    printStatus();
                    break;
                case "join":
                    if (parts.length < 3) {
                        System.out.println("Usage: join <hostname> <port>");
                        break;
                    }
                    myNode.joinNetwork(parts[1], Integer.parseInt(parts[2]));
                    System.out.println("Joined network via " + parts[1] + ":" + parts[2]);
                    break;
                case "leave":
                    myNode.leave();
                    System.out.println("Left the network");
                    break;
                case "list":
                    System.out.println("Known nodes: " + myNode.getKnownNodes());
                    break;
                case "entercs":
                case "request":
                    System.out.println("Requesting critical section (async)...");
                    new Thread(() -> {
                        try {
                            myNode.enterCS();
                            System.out.println("\n>>> ENTERED CRITICAL SECTION <<<");
                            System.out.print("cmd > ");
                        } catch (Exception e) {
                            System.err.println("Failed to enter CS: " + e.getMessage());
                            System.out.print("cmd > ");
                        }
                    }).start();
                    break;
                case "leavecs":
                case "release":
                    myNode.leaveCS();
                    System.out.println("Released critical section");
                    break;
                case "getvar":
                    System.out.println("Shared Variable = " + myNode.getSharedVariable());
                    break;
                case "setvar":
                    if (parts.length < 2) {
                        System.out.println("Usage: setvar <value>");
                        break;
                    }
                    myNode.setSharedVariable(Integer.parseInt(parts[1]));
                    System.out.println("Shared variable set to " + parts[1]);
                    break;
                case "delay":
                    if (parts.length < 2) {
                        System.out.println("Usage: delay <milliseconds>");
                        break;
                    }
                    myNode.setMessageDelayMs(Integer.parseInt(parts[1]));
                    System.out.println("Message delay set to " + parts[1] + "ms");
                    break;
                case "kill":
                    myNode.kill();
                    System.out.println("Node killed (simulated crash)");
                    break;
                case "revive":
                    myNode.revive();
                    System.out.println("Node revived");
                    break;
                case "detect":
                    System.out.println("Detecting dead nodes...");
                    myNode.detectDeadNodes();
                    System.out.println("Detection complete");
                    break;
                case "clock":
                    System.out.println("Logical Clock: " + myNode.getLogicalClock());
                    break;
                case "queue":
                    System.out.println("Request Queue: " + myNode.getQueueStatus());
                    break;
                case "help":
                case "?":
                    printHelp();
                    break;
                case "exit":
                case "quit":
                    System.out.println("Shutting down...");
                    reading = false;
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command: '" + cmd + "'. Type 'help' for available commands.");
            }
        } catch (RemoteException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Command failed: " + e.getMessage());
        }
    }

    private void printStatus() throws RemoteException {
        System.out.println("=== Node Status ===");
        System.out.println("Node ID:         " + myNode.getNodeId());
        System.out.println("Logical Clock:   " + myNode.getLogicalClock());
        System.out.println("In CS:           " + myNode.isInCriticalSection());
        System.out.println("Shared Variable: " + myNode.getSharedVariable());
        System.out.println("Message Delay:   " + myNode.getMessageDelayMs() + "ms");
        System.out.println("Known Nodes:     " + myNode.getKnownNodes().size());
        System.out.println("Request Queue:   " + myNode.getQueueStatus());
    }

    private void printHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("status (s)       - Show node status");
        System.out.println("join <host> <port> - Join network via existing node");
        System.out.println("leave            - Leave the network");
        System.out.println("list             - List known nodes");
        System.out.println("entercs (request) - Request critical section");
        System.out.println("leavecs (release) - Release critical section");
        System.out.println("getvar           - Read shared variable");
        System.out.println("setvar <value>   - Write shared variable (must be in CS)");
        System.out.println("delay <ms>       - Set message delay");
        System.out.println("kill             - Simulate node crash");
        System.out.println("revive           - Revive crashed node");
        System.out.println("detect           - Detect dead nodes");
        System.out.println("clock            - Show logical clock");
        System.out.println("queue            - Show request queue");
        System.out.println("help (?)         - Show this help");
        System.out.println("exit (quit)      - Shutdown node");
        System.out.println();
    }

    @Override
    public void run() {
        System.out.println("\n=== Interactive Console Started ===");
        System.out.println("Type 'help' for available commands\n");

        while (reading) {
            System.out.print("cmd > ");
            try {
                String commandline = reader.readLine();
                if (commandline == null) break; // EOF
                if (!commandline.trim().isEmpty()) {
                    parseCommandLine(commandline);
                }
            } catch (IOException e) {
                System.err.println("Error reading console input: " + e.getMessage());
                reading = false;
            }
        }
        System.out.println("Console closed.");
    }

    public void stop() {
        reading = false;
    }
}