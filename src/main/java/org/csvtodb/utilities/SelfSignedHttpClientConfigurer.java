package org.csvtodb.utilities;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class SelfSignedHttpClientConfigurer implements HttpClientConfigurer {

    /** the logger. */
    private static final Logger LOG = LoggerFactory.getLogger(SelfSignedHttpClientConfigurer.class);

    @Override
    public void configureHttpClient(HttpClientBuilder clientBuilder) {

        try {
            LOG.info("Using SelfSignedHttpClientConfigurer...");

            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true).build();

            clientBuilder.setSSLContext(sslContext)
                    .setConnectionManager(new PoolingHttpClientConnectionManager(RegistryBuilder
                            .<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE)
                            .register("https",
                                    new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                            .build()));

            LOG.info("... HttpClient configured!");

        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            LOG.info(String.valueOf(e));
        }

    }

}