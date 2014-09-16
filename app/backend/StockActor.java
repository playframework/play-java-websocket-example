package backend;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import models.Stock;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;
import utils.FakeStockQuote;
import utils.StockQuote;

import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */
public class StockActor extends AbstractPersistentActor {

    final HashSet<ActorRef> watchers = new HashSet<ActorRef>();

    final Deque<Double> stockHistory = FakeStockQuote.history(50);

    private String symbol;
    private StockQuote stockQuote;
    private boolean tick;

    public StockActor(String symbol) {
        this(symbol, new FakeStockQuote(), true);
    }

    public StockActor(String symbol, StockQuote stockQuote, boolean tick) {
        this.symbol = symbol;
        this.stockQuote = stockQuote;
        this.tick = tick;
    }

    private Cancellable scheduleTick() {
        return context().system().scheduler().schedule(
                Duration.Zero(), Duration.create(75, TimeUnit.MILLISECONDS),
                self(), Stock.latest, context().dispatcher(), null);
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        return ReceiveBuilder.match(Stock.Watch.class, watch -> {
            // reply with the stock history, and add the sender as a watcher
            sender().tell(new Stock.History(symbol, stockHistory), self());
            watchers.add(sender());
        }).build();
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {

        Optional<Cancellable> stockTick = tick ? Optional.of(scheduleTick()) : Optional.empty();
        PartialFunction<Object, BoxedUnit> build = getObjectBoxedUnitPartialFunction(stockTick);
        return build;
    }

    private PartialFunction<Object, BoxedUnit> getObjectBoxedUnitPartialFunction(Optional<Cancellable> stockTick) {
        return ReceiveBuilder
                .match(Stock.Latest.class, latest -> {
                    // add a new stock price to the history and drop the oldest
                    Double newPrice = stockQuote.newPrice(stockHistory.peekLast());
                    stockHistory.add(newPrice);
                    stockHistory.remove();
                    // notify watchers
                    watchers.forEach(watcher -> watcher.tell(new Stock.Update(symbol, newPrice), self()));
                })
                .match(Stock.Watch.class, watch -> {
                    // reply with the stock history, and add the sender as a watcher
                    sender().tell(new Stock.History(symbol, stockHistory), self());
                    watchers.add(sender());
                })
                .match(Stock.Unwatch.class, unwatch -> {
                    watchers.remove(sender());
                    if (watchers.isEmpty()) {
                        stockTick.ifPresent(Cancellable::cancel);
                        context().stop(self());
                    }
                }).build();
    }

    public static class Get {
        final public String symbol;

        public Get(String symbol) {
            this.symbol = symbol;
        }
    }

    public static class EntryEnvelope {
        final public long id;
        final public Object payload;

        public EntryEnvelope(long id, Object payload) {
            this.id = id;
            this.payload = payload;
        }
    }

    @Override
    public String persistenceId() {
        return getSelf().path().parent().parent().name() + "-" + getSelf().path().name();
    }
}
