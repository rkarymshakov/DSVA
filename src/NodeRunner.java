import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.InetAddress;

/**
 * Server class to start a node and register it in RMI registry
 * Usage: java server.Server <port>
 * Example: java server.Server 1099
 */
public class NodeRunner {

    public static void main(String[] args) {
        int port = 2010;

        // Parse command line arguments (only port is required now)
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Usage: java server.Server <port>");
                return;
            }
        }

        try {
            // Get local IP address
            String hostname = InetAddress.getLocalHost().getHostAddress();

            // Generate numeric node ID (exactly like in the example project)
            long nodeId = NodeImpl.generateId(hostname, port);

            System.out.println("=================================================");
            System.out.println("Starting Distributed Node");
            System.out.println("=================================================");
            System.out.println("Node ID: " + nodeId);
            System.out.println("RMI Port: " + port);
            System.out.println("=================================================");

            // Create the node instance with numeric ID
            NodeImpl nodeImpl = new NodeImpl(nodeId);

            // Create or get RMI registry on specified port
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("Created new RMI registry on port " + port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
                System.out.println("Using existing RMI registry on port " + port);
            }

            // Register node under port number (string representation)
            String registryName = String.valueOf(port);
            registry.rebind(registryName, nodeImpl);

            System.out.println("=================================================");
            System.out.println("Node successfully registered in RMI registry");
            System.out.println("Registry name: " + registryName);
            System.out.println("Node is ready and waiting for requests...");
            System.out.println("=================================================");
            System.out.println();
            System.out.println("To connect other nodes to this one, use:");
            System.out.println("  Hostname: " + hostname);
            System.out.println("  Port: " + port);
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}