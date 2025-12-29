package cz.ctu.fee.dsv.semework;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class APIHandler {

    private final NodeImpl node;
    private final int port;
    private Javalin app;

    public APIHandler(NodeImpl node, int port) {
        this.node = node;
        this.port = port;
    }

    public void start() {
        // Start Javalin server
        app = Javalin.create(config -> {
            // config.plugins.enableCors(cors -> cors.add(it -> it.anyHost())); // Optional: if using web browser
        }).start(port);

        System.out.println("REST API started on port " + port);

        // === TOPOLOGY OPERATIONS ===

        // Join: /join/127.0.0.1/2011
        app.post("/join/{ip}/{port}", ctx -> {
            String ip = ctx.pathParam("ip");
            int targetPort = Integer.parseInt(ctx.pathParam("port"));

            try {
                // Connect to the remote node via RMI first
                Registry registry = LocateRegistry.getRegistry(ip, targetPort);
                Node remoteNode = (Node) registry.lookup(String.valueOf(targetPort));

                // Perform the join operation logic from your NodeImpl
                node.join(NodeImpl.generateId(ip, targetPort), remoteNode);

                ctx.result("Joined network via " + ip + ":" + targetPort);
            } catch (Exception e) {
                ctx.status(500).result("Join failed: " + e.getMessage());
            }
        });

        // Leave: /leave
        app.post("/leave", ctx -> {
            node.leave();
            ctx.result("Left the network.");
        });

        // === SIMULATION: FAILURE & RECOVERY ===

        // Kill: /kill (Simulate crash)
        app.post("/kill", ctx -> {
            node.kill();
            ctx.result("Node killed (simulated failure).");
        });

        // Revive: /revive (Restore node)
        app.post("/revive", ctx -> {
            node.revive();
            ctx.result("Node revived.");
        });

        // Delay: /delay/500 (Set message delay in ms)
        app.post("/delay/{ms}", ctx -> {
            int ms = Integer.parseInt(ctx.pathParam("ms"));
            node.setMessageDelayMs(ms);
            ctx.result("Message delay set to " + ms + "ms");
        });

        // Detect: /detect (Run failure detection)
        app.post("/detect", ctx -> {
            node.detectDeadNodes();
            ctx.result("Failure detection cycle triggered.");
        });

        // === ALGORITHM: MUTUAL EXCLUSION ===

        // Enter CS: /enter-cs (Blocking call - waits until entered)
        app.post("/enter-cs", ctx -> {
            // We run this in a blocking way so the HTTP response confirms entry
            try {
                node.enterCS();
                ctx.result("Entered Critical Section");
            } catch (Exception e) {
                ctx.status(500).result("Error entering CS: " + e.getMessage());
            }
        });

        // Leave CS: /leave-cs
        app.post("/leave-cs", ctx -> {
            node.leaveCS();
            ctx.result("Left Critical Section");
        });

        // === SHARED VARIABLE ===

        // Get Variable: /var
        app.get("/var", ctx -> {
            ctx.result(String.valueOf(node.getSharedVariable()));
        });

        // Set Variable: /var/100
        app.post("/var/{value}", ctx -> {
            int val = Integer.parseInt(ctx.pathParam("value"));
            node.setSharedVariable(val);
            ctx.result("Shared variable set to " + val);
        });

        // === STATUS/DEBUG ===

        // Status: /status
        app.get("/status", ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Node ID: ").append(node.getNodeId()).append("\n");
            sb.append("Clock: ").append(node.getLogicalClock()).append("\n");
            sb.append("In CS: ").append(node.isInCriticalSection()).append("\n");
            sb.append("Queue: ").append(node.getQueueStatus()).append("\n");
            sb.append("Known Nodes: ").append(node.getKnownNodes()).append("\n");
            ctx.result(sb.toString());
        });
    }

    public void stop() {
        if (app != null) app.stop();
    }
}