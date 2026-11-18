package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.AgentProvider;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentProviderMapperImpl implements AgentProviderMapper {

    @Override
    public AgentProvider toProto(org.a2aproject.sdk.spec.AgentProvider domain) {
        if ( domain == null ) {
            return null;
        }

        AgentProvider.Builder agentProvider = AgentProvider.newBuilder();

        agentProvider.setUrl( domain.url() );
        agentProvider.setOrganization( domain.organization() );

        return agentProvider.build();
    }

    @Override
    public org.a2aproject.sdk.spec.AgentProvider fromProto(AgentProvider proto) {
        if ( proto == null ) {
            return null;
        }

        String organization = null;
        String url = null;

        organization = proto.getOrganization();
        url = proto.getUrl();

        org.a2aproject.sdk.spec.AgentProvider agentProvider = new org.a2aproject.sdk.spec.AgentProvider( organization, url );

        return agentProvider;
    }
}
