package server;

import compute.Node;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

/**
 * Server class to start a node and register it in RMI registry
 *
 * Usage: java server.Server <port> <nodeName>
 * Example: java server.Server 1099 nodeA
 */
public class Server {

    public static void main(String[] args) {
        // Default values
        int port = 1099;
        String nodeName = "nodeA";

        // Parse command line arguments
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Usage: java server.Server <port> <nodeName>");
                return;
            }
        }

        if (args.length >= 2) {
            nodeName = args[1];
        }

        try {
            // Get local IP address
            String hostname = InetAddress.getLocalHost().getHostAddress();

            // Create unique node ID: nodeName@ip:port
            String nodeId = String.format("%s@%s:%d", nodeName, hostname, port);

            System.out.println("=================================================");
            System.out.println("Starting Distributed Node");
            System.out.println("=================================================");
            System.out.println("Node ID: " + nodeId);
            System.out.println("RMI Port: " + port);
            System.out.println("=================================================");

            // Create the node instance
            NodeImpl nodeImpl = new NodeImpl(nodeId);

            // Create or get RMI registry on specified port
            Registry registry;
            try {
                // Try to create registry (if it doesn't exist)
                registry = LocateRegistry.createRegistry(port);
                System.out.println("Created new RMI registry on port " + port);
            } catch (Exception e) {
                // Registry already exists, just get reference to it
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Using existing RMI registry on port " + port);
            }

            // Register node in registry with its name
            registry.rebind(nodeName, nodeImpl);

            System.out.println("=================================================");
            System.out.println("Node successfully registered in RMI registry");
            System.out.println("Registry name: " + nodeName);
            System.out.println("Node is ready and waiting for requests...");
            System.out.println("=================================================");
            System.out.println();
            System.out.println("To connect other nodes to this one, use:");
            System.out.println("  Hostname: " + hostname);
            System.out.println("  Port: " + port);
            System.out.println("  Name: " + nodeName);
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}