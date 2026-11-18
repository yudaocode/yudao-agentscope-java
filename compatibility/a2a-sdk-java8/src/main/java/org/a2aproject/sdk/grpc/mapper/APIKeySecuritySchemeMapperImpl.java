package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.APIKeySecurityScheme;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class APIKeySecuritySchemeMapperImpl implements APIKeySecuritySchemeMapper {

    @Override
    public org.a2aproject.sdk.grpc.APIKeySecurityScheme toProto(APIKeySecurityScheme domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.APIKeySecurityScheme.Builder aPIKeySecurityScheme = org.a2aproject.sdk.grpc.APIKeySecurityScheme.newBuilder();

        if ( domain.description() != null ) {
            aPIKeySecurityScheme.setDescription( domain.description() );
        }
        if ( domain.location() != null ) {
            aPIKeySecurityScheme.setLocation( domain.location().name() );
        }
        aPIKeySecurityScheme.setName( domain.name() );

        return aPIKeySecurityScheme.build();
    }
}
