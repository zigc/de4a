package eu.de4a.connector.utils;


import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.helger.commons.error.level.EErrorLevel;
import eu.de4a.connector.error.model.ELogMessages;
import eu.de4a.kafkaclient.DE4AKafkaClient;

@Component
public class KafkaClientWrapper {

    @Value("#{'${log.metrics.prefix:DE4A METRICS}'}")
    private static String metricsPrefix;

    private static final String ORIGIN_TAG = "origin";
    private static final String DESTINY_TAG = "destiny";
    private static final String LOG_CODE_TAG = "logcode";
    private static final String METRICS_TAG = "metrics";
    private static final String METRICS_ENABLED_TAG = "metrics.enabled";

    private KafkaClientWrapper (){}

    public static void sendInfo(final ELogMessages logMessage, final Object...params) {
        send(logMessage, EErrorLevel.INFO, params);
    }

    public static void sendSuccess(final ELogMessages logMessage, final Object...params) {
        send(logMessage, EErrorLevel.SUCCESS, params);
    }

    public static void sendWarn(final ELogMessages logMessage, final Object...params) {
        send(logMessage, EErrorLevel.WARN, params);
    }

    public static void sendError(final ELogMessages logMessage, final Object...params) {
        send(logMessage, EErrorLevel.ERROR, params);
    }

    private static CompletableFuture<Void> send(final ELogMessages logMessage, final EErrorLevel level, final Object...params) {
        final String msg = MessageUtils.valueOf(logMessage.getKey(), params);

        ThreadContext.put(ORIGIN_TAG, logMessage.getOrigin().getLabel());
        ThreadContext.put(DESTINY_TAG, logMessage.getDestiny().getLabel());
        ThreadContext.put(METRICS_TAG, metricsPrefix + " >> ");
        ThreadContext.put(METRICS_ENABLED_TAG, "true");
        ThreadContext.put(LOG_CODE_TAG, logMessage.getLogCode());

        return CompletableFuture.runAsync(() -> DE4AKafkaClient.send(level, msg))
            .thenRun(() -> {
                ThreadContext.clearAll();
                ThreadContext.put(METRICS_ENABLED_TAG, "false");
            });

    }

}
