package org.jenkinsci.plugins.gogs;

import hudson.ProxyConfiguration;
import hudson.Util;
import jenkins.model.Jenkins;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.gogs.exceptions.NotificationException;
import org.jenkinsci.plugins.gogs.ext.httpclient.ProxyRoutePlanner;
import org.jenkinsci.plugins.gogs.ext.httpclient.TLSSocketFactory;
import org.jenkinsci.plugins.gogs.model.notifications.Notification;

import java.io.Closeable;
import java.io.IOException;

public abstract class PublishService {

    /**
     * HTTP Connection timeout when making calls to HipChat.
     */
    private static final Integer DEFAULT_TIMEOUT = 10000;

    protected CloseableHttpClient getHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).build())
                .setSSLSocketFactory(new TLSSocketFactory());

        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;

            if (proxy != null) {
                httpClientBuilder.setRoutePlanner(new ProxyRoutePlanner(proxy));
                if (Util.fixEmpty(proxy.getUserName()) != null) {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(proxy.name, proxy.port),
                            new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                }
            }
        }

        return httpClientBuilder.build();
    }


    public abstract void publish(String url, String signature, Notification notification) throws NotificationException;

    protected final String readResponse(HttpEntity entity) throws IOException {
        return entity != null ? EntityUtils.toString(entity) : null;
    }

    protected final void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }
}
