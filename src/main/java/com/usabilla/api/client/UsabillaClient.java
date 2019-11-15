package com.usabilla.api.client;

import com.usabilla.api.utils.CommonUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class UsabillaClient {
    private static final Logger LOGGER = LogManager.getLogger(UsabillaClient.class);

    private static final String TIMEOUT = "timeout";

    private int TimeoutInMs = 10000;
    private int KeepAliveInSec = 60;
    private int MaxThreads = 10;

    public CloseableHttpResponse execute(final HttpUriRequest httpUriRequest) throws IOException {

        // Terminate the request after given timeout value
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                httpUriRequest.abort();
            }
        };
        new Timer(true).schedule(task, (TimeoutInMs) + 100);

        final CloseableHttpClient closeableHttpClient = buildHttpClient();

        return closeableHttpClient.execute(httpUriRequest);
    }

    private CloseableHttpClient buildHttpClient() {
        final ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase(TIMEOUT)) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return KeepAliveInSec * 1000;
        };

        final HttpRoute route = new HttpRoute(new HttpHost(CommonUtils.BASE_URL));

        final PoolingHttpClientConnectionManager connManager
                = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(MaxThreads);
        connManager.setDefaultMaxPerRoute(MaxThreads);
        connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().
                setSoTimeout(TimeoutInMs).build());
        connManager.setValidateAfterInactivity(30 * 1000);

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setRetryHandler(retryHandler())
                .setConnectionTimeToLive(50, TimeUnit.SECONDS)
                .build();
    }

    private HttpRequestRetryHandler retryHandler() {
        return (exception, executionCount, context) -> {

            if (executionCount > 2) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final HttpRequest request = clientContext.getRequest();
            // Retry if the request is considered idempotent
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }

}
