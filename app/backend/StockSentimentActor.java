package backend;

import akka.actor.AbstractActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.ConsistentHashingRouter.ConsistentHashable;
import com.fasterxml.jackson.databind.JsonNode;
import models.Stock;
import play.Play;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import utils.StockQuote;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.toList;
import static utils.Streams.stream;

public class StockSentimentActor extends AbstractActor {

    public static final class GetSentiment implements ConsistentHashable, Serializable {

        static final long serialVersionUID = 42L;

        public final String symbol;

        public GetSentiment(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public Object consistentHashKey() {
            return symbol;
        }
    }

    public static Props props() {
        return Props.create(StockSentimentActor.class, StockSentimentActor::new);
    }


    public StockSentimentActor() {
        receive(ReceiveBuilder
                .match(StockSentimentActor.GetSentiment.class, getSentiment -> {
                    F.Promise<JsonNode> promiseStockSentiments = getStockSentiments(getSentiment.symbol);

                    //Which way is better?
                    //promiseStockSentiments.onRedeem(sentimentJson -> sender().tell(sentimentJson, self()));

                    akka.pattern.Patterns.pipe(promiseStockSentiments.wrapped(), context().dispatcher()).to(sender());

                }).build());
    }


    public F.Promise<JsonNode> getStockSentiments(String symbol) {
        return fetchTweets(symbol)
                .flatMap(tweets -> fetchSentiments(tweets))
                .map(sentiments -> averageSentiment(sentiments));
    }

    public F.Promise<List<String>> fetchTweets(String symbol) {
        return WS.url(Play.application().configuration().getString("tweet.url"))
                .setQueryParameter("q", "$" + symbol).get()
                .filter(response -> response.getStatus() == Http.Status.OK)
                .map(response -> stream(response.asJson().findPath("statuses"))
                        .map(s -> s.findValue("text").asText())
                        .collect(toList()));
    }

    public F.Promise<List<JsonNode>> fetchSentiments(List<String> tweets) {
        String url = Play.application().configuration().getString("sentiment.url");
        Stream<F.Promise<WSResponse>> sentiments = tweets.stream().map(text -> WS.url(url).post("text=" + text));
        return F.Promise
                .sequence(sentiments::iterator)
                .map(responses -> responsesAsJson(responses));
    }

    public List<JsonNode> responsesAsJson(List<WSResponse> responses) {
        return responses.stream().map(WSResponse::asJson).collect(toList());
    }

    public JsonNode averageSentiment(List<JsonNode> sentiments) {
        double neg = collectAverage(sentiments, "neg");
        double neutral = collectAverage(sentiments, "neutral");
        double pos = collectAverage(sentiments, "pos");

        String label = (neutral > 0.5) ? "neutral" : (neg > pos) ? "neg" : "pos";

        return Json.newObject()
                .put("label", label)
                .set("probability", Json.newObject()
                        .put("neg", neg)
                        .put("neutral", neutral)
                        .put("pos", pos));
    }

    public double collectAverage(List<JsonNode> jsons, String label) {
        return jsons.stream().collect(averagingDouble(json -> json.findValue(label).asDouble()));
    }


}
