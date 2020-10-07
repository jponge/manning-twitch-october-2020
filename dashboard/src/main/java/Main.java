import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;

public class Main extends AbstractVerticle {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);
  private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "5000"));

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions()
      .addOutboundPermitted(new PermittedOptions().setAddress("temperature.updates"));
    sockJSHandler.bridge(bridgeOptions);

    router.route("/eventbus/*").handler(sockJSHandler);
    router.route().handler(StaticHandler.create("webroot"));
    router.get("/*").handler(ctx -> ctx.reroute("/index.html"));

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(httpPort)
      .onSuccess(ok -> {
        logger.info("HTTP server running: http://127.0.0.1:{}", httpPort);
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
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
