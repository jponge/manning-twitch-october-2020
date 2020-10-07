import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Main extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "6000"));
  private static final String storeHost = System.getenv().getOrDefault("STORE_HOST", "127.0.0.1");
  private static final int storePort = Integer.parseInt(System.getenv().getOrDefault("STORE_PORT", "7000"));

  private final HashMap<String, Object> lastData = new HashMap<>();
  private WebClient webClient;
  private CircuitBreaker breaker;

  @Override
  public void start(Promise<Void> startPromise) {
    webClient = WebClient.create(vertx);
    breaker = CircuitBreaker.create("store", vertx);

    breaker.openHandler(v -> logger.info("Circuit breaker open"));
    breaker.halfOpenHandler(v -> logger.info("Circuit breaker half-open"));
    breaker.closeHandler(v -> logger.info("Circuit breaker close"));

    vertx.eventBus().consumer("temperature.updates", this::storeUpdate);

    Router router = Router.router(vertx);
    router.get("/latest").handler(this::latestData);
    router.get("/five-minutes").handler(this::fiveMinutes);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(httpPort)
      .onSuccess(ok -> {
        logger.info("HTTP server running: http://127.0.0.1:{}", httpPort);
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  private JsonObject cachedLastFiveMinutes;

  private void fiveMinutes(RoutingContext context) {
    Future<JsonObject> future = breaker.execute(promise -> {
      webClient.get(storePort, storeHost, "/last-5-minutes")
        .expect(ResponsePredicate.SC_OK)
        .as(BodyCodec.jsonObject())
        .timeout(5000)
        .send()
        .map(HttpResponse::body)
        .onSuccess(promise::complete)
        .onFailure(promise::fail);
    });

    future
      .onSuccess(json -> {
        logger.info("Last 5 minutes data requested from {}", context.request().remoteAddress());
        cachedLastFiveMinutes = json;
        context.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(json.encode());
      })
      .onFailure(failure -> {
        logger.info("Last 5 minutes data requested from {} and served from cache", context.request().remoteAddress());
        if (cachedLastFiveMinutes != null) {
          context.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(cachedLastFiveMinutes.encode());
        } else {
          logger.error("Request failed", failure);
          context.fail(500);
        }
      });
  }

  private void latestData(RoutingContext context) {
    logger.info("Latest data requested from {}", context.request().remoteAddress());
    context.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject(lastData).encode());
  }

  private void storeUpdate(Message<JsonObject> message) {
    lastData.put(message.body().getString("uuid"), message.body());
  }


  public static void main(String[] args) {
    Vertx.clusteredVertx(new VertxOptions())
      .onSuccess(vertx -> {
        vertx.deployVerticle(new Main());
        logger.info("Running");
      })
      .onFailure(failure -> logger.error("Not running", failure));
  }
}

