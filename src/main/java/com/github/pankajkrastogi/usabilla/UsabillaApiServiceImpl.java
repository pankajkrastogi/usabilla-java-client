package com.github.pankajkrastogi.usabilla;

import com.github.pankajkrastogi.usabilla.auth.UsabillaAuthBuilder;
import com.github.pankajkrastogi.usabilla.utils.CommonUtils;
import com.github.pankajkrastogi.usabilla.client.UsabillaClient;
import com.github.pankajkrastogi.usabilla.client.model.RequestCommand;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class UsabillaApiServiceImpl implements UsabillaApiService {

    private static final Logger LOGGER = LogManager.getLogger(UsabillaApiServiceImpl.class);

    private static final String ACCEPT = "Accept";

    private int TimeoutInMs = 10000;

    private final UsabillaAuthBuilder usabillaAuthBuilder;

    private final UsabillaClient usabillaClient;

    public UsabillaApiServiceImpl(final UsabillaAuthBuilder usabillaAuthBuilder, final UsabillaClient usabillaClient) {
        this.usabillaAuthBuilder = usabillaAuthBuilder;
        this.usabillaClient = usabillaClient;
    }

    public String getAllFeedbackButtons(final RequestCommand requestCommand) throws Exception {
        final HttpGet httpGet = new HttpGet(buildUri(requestCommand.getUri()));

        try {
            httpGet.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            httpGet.setHeader("Authorization", requestCommand.getAuthorizationHeader());
            httpGet.setHeader("x-usbl-date", requestCommand.getxUsblDateHeader());

            final CloseableHttpResponse response = usabillaClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.debug("Usabilla response status:" + statusCode);

            final HttpEntity entity = response.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            final String responseBody = EntityUtils.toString(entity);
            LOGGER.debug("responseBody:" + responseBody);
            return responseBody;
        } catch (Exception e) {
            throw new InternalError(String.format("Failed to get all feedback buttons due to exception:%s", e));
        }
    }

    private URI buildUri(final String uri) throws URISyntaxException {
        return new URIBuilder(String.join("/", CommonUtils.BASE_URL, uri))
                .build();
    }

    @Override
    public String getFeedbackOnButton(final RequestCommand requestCommand) throws Exception {

        final HttpGet httpGet = new HttpGet(buildUri(requestCommand.getUri()));

        // Terminate the request after given timeout value
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                httpGet.abort();
            }
        };
        new Timer(true).schedule(task, (TimeoutInMs) + 100);

        try {
            httpGet.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            httpGet.setHeader("Authorization", requestCommand.getAuthorizationHeader());
            httpGet.setHeader("x-usbl-date", requestCommand.getxUsblDateHeader());

            final CloseableHttpResponse response = usabillaClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.debug("Usabilla get feedback response status:" + statusCode);

            final HttpEntity entity = response.getEntity();
            if (Objects.isNull(entity)) {
                return null;
            }
            final String responseBody = EntityUtils.toString(entity);
            LOGGER.debug("responseBody:" + responseBody);
            return responseBody;
        } catch (Exception e) {
            throw new InternalError(String.format("Failed to get feedback due to exception:%s", e));
        }
    }

}
