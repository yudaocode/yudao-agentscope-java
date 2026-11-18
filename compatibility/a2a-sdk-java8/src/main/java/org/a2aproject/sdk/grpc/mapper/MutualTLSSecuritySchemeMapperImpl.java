package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.MutualTlsSecurityScheme;
import org.a2aproject.sdk.spec.MutualTLSSecurityScheme;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class MutualTLSSecuritySchemeMapperImpl implements MutualTLSSecuritySchemeMapper {

    @Override
    public MutualTlsSecurityScheme toProto(MutualTLSSecurityScheme domain) {
        if ( domain == null ) {
            return null;
        }

        MutualTlsSecurityScheme.Builder mutualTlsSecurityScheme = MutualTlsSecurityScheme.newBuilder();

        if ( domain.description() != null ) {
            mutualTlsSecurityScheme.setDescription( domain.description() );
        }

        return mutualTlsSecurityScheme.build();
    }
}
