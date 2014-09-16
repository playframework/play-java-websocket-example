package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.FromConfig;
import models.Stock;


public class StockManagerClient extends UntypedActor {
    public static Props props() {
        return Props.create(StockManagerClient.class, StockManagerClient::new);
    }

    private final ActorRef stockManagerRouter =
            getContext().actorOf(Props.empty().withRouter(FromConfig.getInstance()), "router");

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Stock.Watch) {

            stockManagerRouter.forward(msg, context());
        }
    }
}