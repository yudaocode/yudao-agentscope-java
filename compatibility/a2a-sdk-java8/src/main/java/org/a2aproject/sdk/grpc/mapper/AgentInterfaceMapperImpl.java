package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AgentInterface;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentInterfaceMapperImpl implements AgentInterfaceMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.AgentInterface toProto(AgentInterface domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AgentInterface.Builder agentInterface = org.a2aproject.sdk.grpc.AgentInterface.newBuilder();

        if ( domain.tenant() != null ) {
            agentInterface.setTenant( domain.tenant() );
        }
        agentInterface.setUrl( domain.url() );
        agentInterface.setProtocolBinding( domain.protocolBinding() );
        agentInterface.setProtocolVersion( domain.protocolVersion() );

        return agentInterface.build();
    }

    @Override
    public AgentInterface fromProto(org.a2aproject.sdk.grpc.AgentInterface proto) {
        if ( proto == null ) {
            return null;
        }

        String tenant = null;
        String protocolBinding = null;
        String url = null;
        String protocolVersion = null;

        tenant = a2ACommonFieldMapper.emptyToNull( proto.getTenant() );
        protocolBinding = proto.getProtocolBinding();
        url = proto.getUrl();
        protocolVersion = proto.getProtocolVersion();

        AgentInterface agentInterface = new AgentInterface( protocolBinding, url, tenant, protocolVersion );

        return agentInterface;
    }
}
