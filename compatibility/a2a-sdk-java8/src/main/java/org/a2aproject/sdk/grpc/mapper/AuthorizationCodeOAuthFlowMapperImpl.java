package org.a2aproject.sdk.grpc.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AuthorizationCodeOAuthFlow;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AuthorizationCodeOAuthFlowMapperImpl implements AuthorizationCodeOAuthFlowMapper {

    @Override
    public org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow toProto(AuthorizationCodeOAuthFlow domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow.Builder authorizationCodeOAuthFlow = org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow.newBuilder();

        authorizationCodeOAuthFlow.setAuthorizationUrl( domain.authorizationUrl() );
        authorizationCodeOAuthFlow.setTokenUrl( domain.tokenUrl() );
        authorizationCodeOAuthFlow.setRefreshUrl( domain.refreshUrl() );
        authorizationCodeOAuthFlow.setPkceRequired( domain.pkceRequired() );
        if ( authorizationCodeOAuthFlow.getMutableScopes() != null ) {
            Map<String, String> map = domain.scopes();
            if ( map != null ) {
                authorizationCodeOAuthFlow.getMutableScopes().putAll( map );
            }
        }

        return authorizationCodeOAuthFlow.build();
    }

    @Override
    public AuthorizationCodeOAuthFlow fromProto(org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow proto) {
        if ( proto == null ) {
            return null;
        }

        String authorizationUrl = null;
        String refreshUrl = null;
        Map<String, String> scopes = null;
        String tokenUrl = null;
        boolean pkceRequired = false;

        authorizationUrl = proto.getAuthorizationUrl();
        refreshUrl = proto.getRefreshUrl();
        Map<String, String> map = proto.getScopes();
        if ( map != null ) {
            scopes = new LinkedHashMap<String, String>( map );
        }
        tokenUrl = proto.getTokenUrl();
        pkceRequired = proto.getPkceRequired();

        AuthorizationCodeOAuthFlow authorizationCodeOAuthFlow = new AuthorizationCodeOAuthFlow( authorizationUrl, refreshUrl, scopes, tokenUrl, pkceRequired );

        return authorizationCodeOAuthFlow;
    }
}
