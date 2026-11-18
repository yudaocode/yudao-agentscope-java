package org.a2aproject.sdk.grpc.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.ClientCredentialsOAuthFlow;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class ClientCredentialsOAuthFlowMapperImpl implements ClientCredentialsOAuthFlowMapper {

    @Override
    public ClientCredentialsOAuthFlow toProto(org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow domain) {
        if ( domain == null ) {
            return null;
        }

        ClientCredentialsOAuthFlow.Builder clientCredentialsOAuthFlow = ClientCredentialsOAuthFlow.newBuilder();

        clientCredentialsOAuthFlow.setTokenUrl( domain.tokenUrl() );
        clientCredentialsOAuthFlow.setRefreshUrl( domain.refreshUrl() );
        if ( clientCredentialsOAuthFlow.getMutableScopes() != null ) {
            Map<String, String> map = domain.scopes();
            if ( map != null ) {
                clientCredentialsOAuthFlow.getMutableScopes().putAll( map );
            }
        }

        return clientCredentialsOAuthFlow.build();
    }

    @Override
    public org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow fromProto(ClientCredentialsOAuthFlow proto) {
        if ( proto == null ) {
            return null;
        }

        String refreshUrl = null;
        Map<String, String> scopes = null;
        String tokenUrl = null;

        refreshUrl = proto.getRefreshUrl();
        Map<String, String> map = proto.getScopes();
        if ( map != null ) {
            scopes = new LinkedHashMap<String, String>( map );
        }
        tokenUrl = proto.getTokenUrl();

        org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow clientCredentialsOAuthFlow = new org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow( refreshUrl, scopes, tokenUrl );

        return clientCredentialsOAuthFlow;
    }
}
