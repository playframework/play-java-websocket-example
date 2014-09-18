package actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;
import models.Stock;

import akka.event.Logging;
import akka.event.LoggingAdapter;

public class StockManagerClient extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(StockManagerClient.class, StockManagerClient::new);
    }

    private final ActorRef stockManagerRouter =
            getContext().actorOf(Props.empty().withRouter(FromConfig.getInstance()), "router");

    public StockManagerClient() {
        receive(ReceiveBuilder.match(Stock.Watch.class, watch -> {
            log().info("routing Stock.Watch to stockManagerRouter");
            stockManagerRouter.forward(watch, context());
        }).build());
    }
}