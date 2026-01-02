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
    private final NodeImpl myNode;

    public ConsoleHandler(NodeImpl myNode) {
        this.myNode = myNode;
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
                    myNode.joinNetwork((parts[1]), Integer.parseInt(parts[2]));
                    break;
                case "leave":
                    myNode.leave();
                    break;
                case "l":
                    myNode.getKnownNodes().forEach(id -> out.println("Node ID: " + id));
                    break;
                case "s":
                    showStatus();
                    break;
                case "k":
                    myNode.kill();
                    break;
                case "rev":
                    myNode.revive();
                    break;
                case "gv":
                    myNode.getSharedVariable();
                    break;
                case "sv":
                    if (parts.length < 2)
                        break;
                    myNode.setSharedVariable(Integer.parseInt(parts[1]));
                    break;
                case "d":
                    if (parts.length < 2)
                        break;
                    myNode.setMessageDelayMs(Integer.parseInt(parts[1]));
                    break;
                case "det":
                    myNode.detectDeadNodes();
                    break;
                case "req":
                    myNode.enterCS();
                    break;
                case "rel":
                    myNode.leaveCS();
                    break;
                case "?":
                    printHelp();
                    break;
                default:
                    out.println("Unrecognized command. Type '?' for available commands");
            }
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
        }
    }

    private void showStatus() {
        try {
            out.println("Node ID: " + myNode.getNodeId());
            out.println("Logical Clock: " + myNode.getLogicalClock());
            out.println("In CS: " + myNode.isInCriticalSection());
            out.println("Request Queue: " + myNode.getQueueStatus());
            out.println("Message Delay: " + myNode.getMessageDelayMs() + "ms");
            out.println("Known Nodes: " + myNode.getKnownNodes().size());
            out.println("Shared Variable: " + myNode.getSharedVariable());
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
                out.print("[Node " + myNode.getNodeId() + "]> ");
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