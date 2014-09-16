package actors;


import akka.actor.ActorRef;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import backend.StockSentimentActor;
import backend.journal.SharedJournalSetter;

public class ActorManagerExtensionImpl implements Extension {

    private final ActorRef stockManagerClient;
    private final ActorRef stockSentimentActor;

    public ActorManagerExtensionImpl(ExtendedActorSystem system) {
        stockManagerClient = system.actorOf(StockManagerClient.props(), "stockManagerClient");
        stockSentimentActor = system.actorOf(StockSentimentActor.props(), "stockSentimentActor");

        //The shared journal needs to be started on every node in the cluster.  This will (should) start it for
        //Play - since this extension is used by Play.
        system.actorOf(SharedJournalSetter.props(), "shared-journal-setter");
    }

    public ActorRef getStockManagerClient() {
        return stockManagerClient;
    }

    public ActorRef getStockSentimentActor() {
        return stockSentimentActor;
    }
}
