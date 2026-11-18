package org.a2aproject.sdk.client.http;

/**
 * Registers the default Java 8 HTTP transport for the repackaged A2A SDK.
 *
 * @author 芋道源码
 */
public final class Java8A2AHttpClientProvider implements A2AHttpClientProvider {

    @Override
    public A2AHttpClient create() {
        return create(A2AHttpClientConfiguration.DEFAULT);
    }

    @Override
    public A2AHttpClient create(A2AHttpClientConfiguration configuration) {
        return new Java8A2AHttpClient(
                configuration.sseParserConfigOrDefault(), configuration.redirectHeaderPolicyOrDefault());
    }

    @Override
    public boolean supportsSseConfig() {
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String name() {
        return "jdk";
    }
}
