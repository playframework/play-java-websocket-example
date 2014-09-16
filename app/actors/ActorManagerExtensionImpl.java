package actors;


import akka.actor.ActorRef;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;

public class ActorManagerExtensionImpl implements Extension {

    private final ActorRef stockManagerClient;

    public ActorManagerExtensionImpl(ExtendedActorSystem system) {
        stockManagerClient = system.actorOf(StockManagerClient.props(), "stockManagerClient");
    }

    public ActorRef getStockManagerClient() {
        return stockManagerClient;
    }
}
