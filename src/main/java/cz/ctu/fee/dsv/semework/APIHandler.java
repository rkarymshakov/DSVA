package cz.ctu.fee.dsv.semework;

import io.javalin.Javalin;

public class APIHandler {
    private final NodeImpl node;
    private final int port;
    private Javalin app;

    public APIHandler(NodeImpl node, int port) {
        this.node = node;
        this.port = port;
    }

    public void start() {
        app = Javalin.create().start(port);

        System.out.println("REST API started on port " + port);

        app.post("/join/{ip}/{port}", ctx -> {
            String ip = ctx.pathParam("ip");
            int targetPort = Integer.parseInt(ctx.pathParam("port"));

            System.out.println("Joining node via: " + ip + ":" + targetPort);

            try {
                node.joinNetwork(ip, targetPort);
                ctx.result("Joined network via " + ip + ":" + targetPort + "\n");
            } catch (Exception e) {
                ctx.status(500).result("Join failed: " + e.getMessage());
            }
        });

        app.post("/leave", ctx -> {
            node.leave();
            ctx.result("Left the network.");
        });

        app.post("/kill", ctx -> {
            node.kill();
            ctx.result("Node killed (simulated failure).");
        });

        app.post("/revive", ctx -> {
            node.revive();
            ctx.result("Node revived.");
        });

        app.post("/delay/{ms}", ctx -> {
            int ms = Integer.parseInt(ctx.pathParam("ms"));
            node.setMessageDelayMs(ms);
            ctx.result("Message delay set to " + ms + "ms");
        });

        app.post("/detect", ctx -> {
            node.detectDeadNodes();
            ctx.result("Failure detection cycle triggered.");
        });

        app.post("/enter-cs", ctx -> {
            try { // run this in a blocking way so the HTTP response confirms entry
                node.enterCS();
                ctx.result("Entered Critical Section");
            } catch (Exception e) {
                ctx.status(500).result("Error entering CS: " + e.getMessage());
            }
        });

        app.post("/leave-cs", ctx -> {
            node.leaveCS();
            ctx.result("Left Critical Section");
        });

        app.get("/var", ctx -> ctx.result(String.valueOf(node.getSharedVariable())));

        app.post("/var/{value}", ctx -> {
            int val = Integer.parseInt(ctx.pathParam("value"));
            node.setSharedVariable(val);
            ctx.result("Shared variable set to " + val);
        });

        app.get("/status", ctx -> {
            String sb = "Node ID: " + node.getNodeId() + "\n" +
                    "Clock: " + node.getLogicalClock() + "\n" +
                    "In CS: " + node.isInCriticalSection() + "\n" +
                    "Queue: " + node.getQueueStatus() + "\n" +
                    "Known Nodes: " + node.getKnownNodes() + "\n";
            ctx.result(sb);
        });
    }

    public void stop() {
        if (app != null) app.stop();
    }
}