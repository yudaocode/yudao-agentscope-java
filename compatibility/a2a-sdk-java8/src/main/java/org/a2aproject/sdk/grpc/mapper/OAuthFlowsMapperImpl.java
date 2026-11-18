package org.a2aproject.sdk.grpc.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.DeviceCodeOAuthFlow;
import org.a2aproject.sdk.grpc.OAuthFlows;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class OAuthFlowsMapperImpl implements OAuthFlowsMapper {

    private final AuthorizationCodeOAuthFlowMapper authorizationCodeOAuthFlowMapper = AuthorizationCodeOAuthFlowMapper.INSTANCE;
    private final ClientCredentialsOAuthFlowMapper clientCredentialsOAuthFlowMapper = ClientCredentialsOAuthFlowMapper.INSTANCE;

    @Override
    public OAuthFlows toProto(org.a2aproject.sdk.spec.OAuthFlows domain) {
        if ( domain == null ) {
            return null;
        }

        OAuthFlows.Builder oAuthFlows = OAuthFlows.newBuilder();

        oAuthFlows.setAuthorizationCode( authorizationCodeOAuthFlowMapper.toProto( domain.authorizationCode() ) );
        oAuthFlows.setClientCredentials( clientCredentialsOAuthFlowMapper.toProto( domain.clientCredentials() ) );
        oAuthFlows.setDeviceCode( deviceCodeOAuthFlowToDeviceCodeOAuthFlow( domain.deviceCode() ) );

        return oAuthFlows.build();
    }

    @Override
    public org.a2aproject.sdk.spec.OAuthFlows fromProto(OAuthFlows proto) {
        if ( proto == null ) {
            return null;
        }

        org.a2aproject.sdk.spec.OAuthFlows.Builder oAuthFlows = org.a2aproject.sdk.spec.OAuthFlows.builder();

        if ( proto.hasAuthorizationCode() ) {
            oAuthFlows.authorizationCode( authorizationCodeOAuthFlowMapper.fromProto( proto.getAuthorizationCode() ) );
        }
        if ( proto.hasClientCredentials() ) {
            oAuthFlows.clientCredentials( clientCredentialsOAuthFlowMapper.fromProto( proto.getClientCredentials() ) );
        }
        if ( proto.hasDeviceCode() ) {
            oAuthFlows.deviceCode( deviceCodeOAuthFlowToDeviceCodeOAuthFlow1( proto.getDeviceCode() ) );
        }

        return oAuthFlows.build();
    }

    protected DeviceCodeOAuthFlow deviceCodeOAuthFlowToDeviceCodeOAuthFlow(org.a2aproject.sdk.spec.DeviceCodeOAuthFlow deviceCodeOAuthFlow) {
        if ( deviceCodeOAuthFlow == null ) {
            return null;
        }

        DeviceCodeOAuthFlow.Builder deviceCodeOAuthFlow1 = DeviceCodeOAuthFlow.newBuilder();

        deviceCodeOAuthFlow1.setDeviceAuthorizationUrl( deviceCodeOAuthFlow.deviceAuthorizationUrl() );
        deviceCodeOAuthFlow1.setTokenUrl( deviceCodeOAuthFlow.tokenUrl() );
        deviceCodeOAuthFlow1.setRefreshUrl( deviceCodeOAuthFlow.refreshUrl() );
        if ( deviceCodeOAuthFlow1.getMutableScopes() != null ) {
            Map<String, String> map = deviceCodeOAuthFlow.scopes();
            if ( map != null ) {
                deviceCodeOAuthFlow1.getMutableScopes().putAll( map );
            }
        }

        return deviceCodeOAuthFlow1.build();
    }

    protected org.a2aproject.sdk.spec.DeviceCodeOAuthFlow deviceCodeOAuthFlowToDeviceCodeOAuthFlow1(DeviceCodeOAuthFlow deviceCodeOAuthFlow) {
        if ( deviceCodeOAuthFlow == null ) {
            return null;
        }

        String deviceAuthorizationUrl = null;
        String tokenUrl = null;
        String refreshUrl = null;
        Map<String, String> scopes = null;

        deviceAuthorizationUrl = deviceCodeOAuthFlow.getDeviceAuthorizationUrl();
        tokenUrl = deviceCodeOAuthFlow.getTokenUrl();
        refreshUrl = deviceCodeOAuthFlow.getRefreshUrl();
        Map<String, String> map = deviceCodeOAuthFlow.getScopes();
        if ( map != null ) {
            scopes = new LinkedHashMap<String, String>( map );
        }

        org.a2aproject.sdk.spec.DeviceCodeOAuthFlow deviceCodeOAuthFlow1 = new org.a2aproject.sdk.spec.DeviceCodeOAuthFlow( deviceAuthorizationUrl, tokenUrl, refreshUrl, scopes );

        return deviceCodeOAuthFlow1;
    }
}
