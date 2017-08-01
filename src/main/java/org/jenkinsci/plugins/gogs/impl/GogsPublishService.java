package org.jenkinsci.plugins.gogs.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jenkinsci.plugins.gogs.Messages;
import org.jenkinsci.plugins.gogs.PublishService;
import org.jenkinsci.plugins.gogs.exceptions.InvalidResponseCodeException;
import org.jenkinsci.plugins.gogs.exceptions.NotificationException;
import org.jenkinsci.plugins.gogs.model.notifications.Notification;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jeff on 01/08/2017.
 */
public class GogsPublishService extends PublishService {

    private static final Logger LOGGER = Logger.getLogger(GogsPublishService.class.getName());
    private static final ObjectWriter writer = new ObjectMapper().writerWithView(Notification.class);

    @Override
    public void publish(Notification notification) throws NotificationException {
        CloseableHttpClient httpClient = getHttpClient();
        CloseableHttpResponse httpResponse = null;
        try {
            String url = "http://localhost:3000/kuwago/jhipster-showcase/pipeline/callback";
            String token = "";
            HttpPost post = new HttpPost(url);
            post.addHeader("Authorization", "Bearer " + token);
            post.setEntity(new StringEntity(writer.writeValueAsString(notification), ContentType.APPLICATION_JSON));

            httpResponse = httpClient.execute(post);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            // Always read response to ensure the inputstream is closed
            String response = readResponse(httpResponse.getEntity());

            if (responseCode != 204) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "HipChat post may have failed. ResponseCode: {0}, Response: {1}",
                            new Object[]{responseCode, response});
                    throw new InvalidResponseCodeException(responseCode);
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "An IO error occurred while posting HipChat notification", ioe);
            throw new NotificationException(Messages.IOException(ioe.toString()));
        } finally {
            closeQuietly(httpResponse, httpClient);
        }
    }
}
