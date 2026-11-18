package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AuthenticationInfo;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AuthenticationInfoMapperImpl implements AuthenticationInfoMapper {

    @Override
    public org.a2aproject.sdk.grpc.AuthenticationInfo toProto(AuthenticationInfo domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AuthenticationInfo.Builder authenticationInfo = org.a2aproject.sdk.grpc.AuthenticationInfo.newBuilder();

        if ( domain.credentials() != null ) {
            authenticationInfo.setCredentials( domain.credentials() );
        }
        authenticationInfo.setScheme( domain.scheme() );

        return authenticationInfo.build();
    }

    @Override
    public AuthenticationInfo fromProto(org.a2aproject.sdk.grpc.AuthenticationInfo proto) {
        if ( proto == null ) {
            return null;
        }

        String scheme = null;
        String credentials = null;

        scheme = proto.getScheme();
        credentials = proto.getCredentials();

        AuthenticationInfo authenticationInfo = new AuthenticationInfo( scheme, credentials );

        return authenticationInfo;
    }
}
