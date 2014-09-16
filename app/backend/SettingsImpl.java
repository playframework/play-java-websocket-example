package backend;

import akka.actor.Extension;
import com.typesafe.config.Config;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class SettingsImpl implements Extension {

    SettingsImpl(Config config) {
        this.SentimentUrl = config.getString("sentiment.url");
        this.TweetUrl = config.getString("tweet.url");
    }


  public final String SentimentUrl;
  public final String TweetUrl;
}
