package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.OpenIdConnectSecurityScheme;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class OpenIdConnectSecuritySchemeMapperImpl implements OpenIdConnectSecuritySchemeMapper {

    @Override
    public OpenIdConnectSecurityScheme toProto(org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme domain) {
        if ( domain == null ) {
            return null;
        }

        OpenIdConnectSecurityScheme.Builder openIdConnectSecurityScheme = OpenIdConnectSecurityScheme.newBuilder();

        if ( domain.description() != null ) {
            openIdConnectSecurityScheme.setDescription( domain.description() );
        }
        openIdConnectSecurityScheme.setOpenIdConnectUrl( domain.openIdConnectUrl() );

        return openIdConnectSecurityScheme.build();
    }
}
