package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.FromConfig;
import models.Stock;

import akka.event.Logging;
import akka.event.LoggingAdapter;

public class StockManagerClient extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static Props props() {
        return Props.create(StockManagerClient.class, StockManagerClient::new);
    }

    private final ActorRef stockManagerRouter =
            getContext().actorOf(Props.empty().withRouter(FromConfig.getInstance()), "router");

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Stock.Watch) {
            log.info("routing Stock.Watch to stockManagerRouter");
            stockManagerRouter.forward(msg, context());
        }
    }
}