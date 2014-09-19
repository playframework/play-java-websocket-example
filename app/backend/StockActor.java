package backend;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.dispatch.*;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.pattern.Patterns$;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import akka.util.Timeout;
import models.Stock;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;
import utils.FakeStockQuote;
import utils.StockQuote;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static akka.pattern.Patterns.ask;
import akka.actor.Identify;


/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */
public class StockActor extends AbstractPersistentActor {

    private Set<ActorRef> watchers = new HashSet<>();

    private Deque<Double> stockHistory = FakeStockQuote.history(50);

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

    private Cancellable snapshotTick() {
        return context().system().scheduler().schedule(
                Duration.Zero(), Duration.create(5, TimeUnit.MINUTES),
                self(), Events.snap, context().dispatcher(), null);
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receiveRecover() {
        System.out.println(">>>>>>>>>>>> ");
        System.out.println(">>>>>>>>>>>> Running receiveRecover " + symbol);
        System.out.println(">>>>>>>>>>>> ");

        return ReceiveBuilder
                .match(Events.StockPriceUpdated.class, this::updateHistory)
                .match(Events.WatcherAdded.class, r -> addWatcherIfAlive(r.watcher()))
                .match(Events.WatcherRemoved.class, this::removeWatcher)
                .match(SnapshotOffer.class, this::updateState)
                .build();
    }




    @Override
    public PartialFunction<Object, BoxedUnit> receiveCommand() {

        Optional<Cancellable> stockTick = tick ? Optional.of(scheduleTick()) : Optional.empty();
        Cancellable snapShotTick = snapshotTick();

        return ReceiveBuilder
                .match(Stock.Latest.class, latest -> {
                    Double newPrice = stockQuote.newPrice(stockHistory.peekLast());
                    //too many persistence messages. Maybe its good idea to capture that as a snapshot
//                    persist(new Events.StockPriceUpdated(newPrice), evt -> {
//                        updateHistory(evt);
                        // notify watchers
                        watchers.forEach(watcher -> watcher.tell(new Stock.Update(symbol, newPrice), self()));
//                    });
                })
                .match(Events.AddWatcherAfterRecover.class, evt -> {
                   watchers.add(evt.watcher()); //only used to add watchers after recover
                })
                .match(Stock.Watch.class, watch -> {
                    persist(new Events.WatcherAdded(sender()), evt -> {
                        addWatcher(evt);
                        // reply with the stock history, and add the sender as a watcher
                        evt.watcher().tell(new Stock.History(symbol, stockHistory), self());
                    });
                })
                .match(Stock.Unwatch.class, unwatch -> {
                    persist(new Events.WatcherRemoved(sender()), evt -> {
                        removeWatcher(evt);
                        if (watchers.isEmpty()) {
                            stockTick.ifPresent(Cancellable::cancel);
                            snapShotTick.cancel();
                            context().stop(self());
                        }

                    });
                })
                .match(Events.Snap.class, s -> {
                    saveSnapshot(new Events.TakeSnapshot(stockHistory, watchers));
                })
                .build();
    }


    private void updateState(SnapshotOffer offer) {
        Events.TakeSnapshot snapshot = (Events.TakeSnapshot)offer.snapshot();
        stockHistory = snapshot.history;
        for(ActorRef w : snapshot.watchers) {
           addWatcherIfAlive(w);
        }
    }

    private void updateHistory(Events.StockPriceUpdated evt) {
        stockHistory.add(evt.price());
        stockHistory.remove();
    }

    private void addWatcher(Events.WatcherAdded evt) {
        watchers.add(evt.watcher());
    }

    private void addWatcherIfAlive(ActorRef w) {
        //using actor identity to make sure the watcher is still alive after recovery
        Patterns.ask(w, new Identify(w.path().name()), 100)
           .map(new Mapper<Object, Void>() {
               @Override
               public Void apply(Object result) {
                   ActorIdentity ai = (ActorIdentity) result;
                   if (ai.getRef() != null) {
                       self().tell(new Events.AddWatcherAfterRecover(w), ActorRef.noSender());
                   }
                   return null;
               }
           }, context().dispatcher())
          .recover(new Recover<Void>() {
              public Void recover(Throwable failure) {
                  self().tell(new Stock.Unwatch(Optional.of(symbol)), w);
                  return null;
              }
          }, context().dispatcher());
    }

    private void removeWatcher(Events.WatcherRemoved evt) {
        watchers.remove(evt.watcher());
    }

    public static class Events {

        public static Snap snap = new Snap();

        public static class StockPriceUpdated implements Serializable {
            private static final long serialVersionUID = 101L;

            final private Double price;
            public StockPriceUpdated(Double newPrice) { this.price = newPrice; }

            public Double price() { return price; }
        }

        public static class WatcherAdded implements Serializable {
            private static final long serialVersionUID = 202L;
            final private ActorRef watcher;
            public WatcherAdded(ActorRef watcher) { this.watcher = watcher; }

            public ActorRef watcher() { return watcher; }
        }

        public static class WatcherRemoved implements Serializable  {
            private static final long serialVersionUID = 303L;
            final private ActorRef watcher;
            public WatcherRemoved(ActorRef watcher) { this.watcher = watcher; }

            public ActorRef watcher() { return watcher; }
        }

        public static class TakeSnapshot implements Serializable {
            private static final long serialVersionUID = 404L;

            private Deque<Double> history;
            private Set<ActorRef> watchers;

            public TakeSnapshot(Deque<Double> history, Set<ActorRef> watchers) {
              this.history = history;
              this.watchers = watchers;
            }

            public Deque<Double> history() { return history; }

            public Set<ActorRef> watchers() { return watchers; }
        }


        public static class AddWatcherAfterRecover {
            private ActorRef watcher;
            public AddWatcherAfterRecover(ActorRef w)  {
                this.watcher = w;
            }

            public ActorRef watcher() { return watcher; }
        }

        public static class Snap {}
    }

    @Override
    public String persistenceId() {
        return "symbol_" + symbol;
    }
}
