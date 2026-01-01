package cz.ctu.fee.dsv.semwork;

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

        // === 1. Calculate REST Port (Offset by 1000) ===
        int restPort = rmiPort + 1000;

        try {
            String hostname = InetAddress.getLocalHost().getHostAddress();
            long nodeId = NodeImpl.generateId(hostname, rmiPort);

            System.out.println("Starting Node ID: " + nodeId);
            NodeImpl nodeImpl = new NodeImpl(nodeId);

            // === 2. Start REST API ===
            APIHandler apiHandler = new APIHandler(nodeImpl, restPort);
            apiHandler.start();

            // Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down...");
                apiHandler.stop(); // Stop REST
                nodeImpl.shutdown();
            }));

            // === 3. RMI Setup (Existing Code) ===
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(rmiPort);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(rmiPort);
            }
            registry.rebind(String.valueOf(rmiPort), nodeImpl);

            System.out.println("RMI Registry: port " + rmiPort);
            System.out.println("REST API:     port " + restPort);
            System.out.println("Ready.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}