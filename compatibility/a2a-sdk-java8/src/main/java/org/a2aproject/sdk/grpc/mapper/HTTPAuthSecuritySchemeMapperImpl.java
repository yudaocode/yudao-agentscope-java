package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class HTTPAuthSecuritySchemeMapperImpl implements HTTPAuthSecuritySchemeMapper {

    @Override
    public org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme toProto(HTTPAuthSecurityScheme domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme.Builder hTTPAuthSecurityScheme = org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme.newBuilder();

        if ( domain.bearerFormat() != null ) {
            hTTPAuthSecurityScheme.setBearerFormat( domain.bearerFormat() );
        }
        if ( domain.description() != null ) {
            hTTPAuthSecurityScheme.setDescription( domain.description() );
        }
        hTTPAuthSecurityScheme.setScheme( domain.scheme() );

        return hTTPAuthSecurityScheme.build();
    }
}
