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
        ActorSystem system = null;
        if (args.length < 2) {
            system = startSystem("0", "backend");
        } else {
            String port = args[0];
            String role = args[1];
            system = startSystem(port, role);
        }

        if (Cluster.get(system).getSelfRoles().stream().anyMatch(r -> r.startsWith("backend"))) {
            system.actorOf(StockManager.props(), "stockManager");
        }

        commandLoop(system);
    }

    public static ActorSystem startSystem(String port, String role) {

        int portNr = Integer.parseInt(port);

        // Override the port number configuration
        Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + portNr).
                withFallback(ConfigFactory.parseString("akka.cluster.roles=[" + role + "]")).
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
