package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class OAuth2SecuritySchemeMapperImpl implements OAuth2SecuritySchemeMapper {

    private final OAuthFlowsMapper oAuthFlowsMapper = OAuthFlowsMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.OAuth2SecurityScheme toProto(OAuth2SecurityScheme domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.OAuth2SecurityScheme.Builder oAuth2SecurityScheme = org.a2aproject.sdk.grpc.OAuth2SecurityScheme.newBuilder();

        if ( domain.description() != null ) {
            oAuth2SecurityScheme.setDescription( domain.description() );
        }
        if ( domain.oauth2MetadataUrl() != null ) {
            oAuth2SecurityScheme.setOauth2MetadataUrl( domain.oauth2MetadataUrl() );
        }
        oAuth2SecurityScheme.setFlows( oAuthFlowsMapper.toProto( domain.flows() ) );

        return oAuth2SecurityScheme.build();
    }
}
