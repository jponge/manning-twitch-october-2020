import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Vertx.clusteredVertx(new VertxOptions())
      .onSuccess(vertx -> vertx.deployVerticle(new SensorVerticle())
        .onSuccess(id -> logger.info("✅ Started"))
        .onFailure(failure -> logger.error("🚨 Deployment failed", failure)))
      .onFailure(failure -> logger.error("🚨 Start failed", failure));
  }
}
