package ctu.dsv.semwork;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

public class NodeRunner {
    public static void main(String[] args) {
        int rmiPort = 2010;

        if (args.length >= 1) {
            try {
                rmiPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port.");
                return;
            }
        }

        int restPort = rmiPort + 1000;

        try {
            String hostname = InetAddress.getLocalHost().getHostAddress();
            long nodeId = NodeImpl.generateId(hostname, rmiPort);

            System.out.println("Starting Node ID: " + nodeId);
            NodeImpl nodeImpl = new NodeImpl(nodeId);

            APIHandler apiHandler = new APIHandler(nodeImpl, restPort);
            apiHandler.start();

            ConsoleHandler consoleHandler = new ConsoleHandler(nodeImpl);
            Thread consoleThread = new Thread(consoleHandler);
            consoleThread.start();

            // Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                consoleHandler.stop();
                apiHandler.stop();
                nodeImpl.shutdown();
            }));

            // RMI Setup
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(rmiPort);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(rmiPort);
            }
            registry.rebind(String.valueOf(rmiPort), nodeImpl);

            System.out.println("RMI Registry: port " + rmiPort);
            System.out.println("REST API:     port " + restPort);
            System.out.println("Node Ready.\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}