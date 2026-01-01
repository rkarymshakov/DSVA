package ctu.dsv.semwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.RemoteException;

public class ConsoleHandler implements Runnable {
    private boolean reading = true;
    private final BufferedReader reader;
    private final PrintStream out = System.out;
    private final PrintStream err = System.err;
    private final NodeImpl currentNode;

    public ConsoleHandler(NodeImpl myNode) {
        this.currentNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    private void parse_commandline(String commandline) {
        if (commandline.trim().isEmpty()) return;

        String[] parts = commandline.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "j":
                    if (parts.length < 3)
                        break;
                    currentNode.joinNetwork((parts[1]), Integer.parseInt(parts[2]));
                    break;
                case "leave":
                    currentNode.leave();
                    break;
                case "l":
                    currentNode.getKnownNodes().forEach(id -> out.println("Node ID: " + id));
                    break;
                case "s":
                    showStatus();
                    break;
                case "k":
                    currentNode.kill();
                    break;
                case "rev":
                    currentNode.revive();
                    break;
                case "gv":
                    currentNode.getSharedVariable();
                    break;
                case "sv":
                    if (parts.length < 2)
                        break;
                    currentNode.setSharedVariable(Integer.parseInt(parts[1]));
                    break;
                case "d":
                    if (parts.length < 2)
                        break;
                    currentNode.setMessageDelayMs(Integer.parseInt(parts[1]));
                    break;
                case "det":
                    currentNode.detectDeadNodes();
                    break;
                case "req":
                    currentNode.enterCS();
                    break;
                case "rel":
                    currentNode.leaveCS();
                    break;
                case "?":
                    printHelp();
                    break;
                default:
                    out.println("Unrecognized command. Type 'help' for available commands");
            }
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
        }
    }

    private void showStatus() {
        try {
            out.println("Node ID: " + currentNode.getNodeId());
            out.println("Logical Clock: " + currentNode.getLogicalClock());
            out.println("In CS: " + currentNode.isInCriticalSection());
            out.println("Request Queue: " + currentNode.getQueueStatus());
            out.println("Message Delay: " + currentNode.getMessageDelayMs() + "ms");
            out.println("Known Nodes: " + currentNode.getKnownNodes().size());
        } catch (Exception e) { err.println("Error: " + e.getMessage()); }
    }

    private void printHelp() {
        out.println("Commands:");
        out.println("j <host> <port>       - Join network via node");
        out.println("leave                 - Leave network");
        out.println("l                     - List known nodes");
        out.println("req                   - Request critical section");
        out.println("rel                   - Release critical section");
        out.println("gv                    - Get shared variable");
        out.println("sv <value>            - Set shared variable");
        out.println("s                     - Show node status");
        out.println("c                     - Show logical clock");
        out.println("d <ms>                - Set message delay");
        out.println("k                     - Simulate node crash");
        out.println("rev                   - Revive crashed node");
        out.println("det                   - Detect dead nodes");
        out.println("?                     - Show this help");
    }

    @Override
    public void run() {
        String commandline;
        printHelp();

        while (reading) {
            try {
                out.print("[Node " + currentNode.getNodeId() + "]> ");
            } catch (RemoteException e) {
                throw new RuntimeException(e);

            }
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