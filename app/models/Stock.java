package models;



import akka.routing.ConsistentHashingRouter.ConsistentHashable;
import java.io.Serializable;
import java.util.Deque;
import java.util.Optional;

public class Stock implements Serializable {

    static final long serialVersionUID = 42L;

    public static final class Latest {
        public Latest() {}
    }

    public static final Latest latest = new Latest();

    public static final class Update implements Serializable {

        static final long serialVersionUID = Stock.serialVersionUID;

        public final String symbol;
        public final Double price;

        public Update(String symbol, Double price) {
            this.symbol = symbol;
            this.price = price;
        }
    }

    public static final class History implements Serializable {

        static final long serialVersionUID = Stock.serialVersionUID;

        public final String symbol;
        public final Deque<Double> history;

        public History(String symbol, Deque<Double> history) {
            this.symbol = symbol;
            this.history = history;
        }
    }

    public static final class Watch implements ConsistentHashable, Serializable {

        static final long serialVersionUID = Stock.serialVersionUID;

        public final String symbol;

        public Watch(String symbol) {
            this.symbol = symbol;
        }


        @Override
        public Object consistentHashKey() {
            return symbol;
        }
    }

    public static final class Unwatch implements ConsistentHashable, Serializable  {

        static final long serialVersionUID = Stock.serialVersionUID;

        public final Optional<String> symbol;

        public Unwatch(Optional<String> symbol) {
            this.symbol = symbol;
        }


        @Override
        public Object consistentHashKey() {
            return symbol;
        }
    }
}
