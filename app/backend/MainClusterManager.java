package backend;

import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainClusterManager {

    public static void main(String... args) throws IOException {

        //improper / lazy checking - export port and cluster - or use defaults if not available
        ActorSystem system = startSystem(args);
        if (Cluster.get(system).getSelfRoles().stream().anyMatch(r -> r.startsWith("backend"))) {
            system.actorOf(StockManager.props(), "stockManager");
        }

        commandLoop(system);
    }

    private static ActorSystem startSystem(String... cmdArgs) {
        if (cmdArgs.length < 1) {
            return startSystem("backend");
        } else {
            String role = cmdArgs[0];
            return startSystem(role);
        }

    }

    public static ActorSystem startSystem(String role) {
        // Override the port number configuration
        Config config = ConfigFactory.parseString("akka.cluster.roles=[" + role + "]").
                withFallback(ConfigFactory.parseString("akka.loglevel=INFO")).
                withFallback(ConfigFactory.load());

        return ActorSystem.create("application", config);
    }

    public static void commandLoop(ActorSystem system) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter `s` to shutdown:");
        String s = br.readLine();
        if (s.startsWith("s")) {
            system.shutdown();
        }
        else {
            commandLoop(system);
        }
    }
}
