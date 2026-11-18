package org.a2aproject.sdk.grpc.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentExtension;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentCapabilitiesMapperImpl implements AgentCapabilitiesMapper {

    private final AgentExtensionMapper agentExtensionMapper = AgentExtensionMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.AgentCapabilities toProto(AgentCapabilities domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AgentCapabilities.Builder agentCapabilities = org.a2aproject.sdk.grpc.AgentCapabilities.newBuilder();

        agentCapabilities.setStreaming( domain.streaming() );
        agentCapabilities.setPushNotifications( domain.pushNotifications() );
        agentCapabilities.setExtendedAgentCard( domain.extendedAgentCard() );
        if ( domain.extensions() != null ) {
            for ( AgentExtension extension : domain.extensions() ) {
                agentCapabilities.addExtensions( agentExtensionMapper.toProto( extension ) );
            }
        }

        return agentCapabilities.build();
    }

    @Override
    public AgentCapabilities fromProto(org.a2aproject.sdk.grpc.AgentCapabilities proto) {
        if ( proto == null ) {
            return null;
        }

        AgentCapabilities.Builder agentCapabilities = AgentCapabilities.builder();

        if ( proto.hasStreaming() ) {
            agentCapabilities.streaming( proto.getStreaming() );
        }
        if ( proto.hasPushNotifications() ) {
            agentCapabilities.pushNotifications( proto.getPushNotifications() );
        }
        if ( proto.hasExtendedAgentCard() ) {
            agentCapabilities.extendedAgentCard( proto.getExtendedAgentCard() );
        }
        agentCapabilities.extensions( agentExtensionListToAgentExtensionList( proto.getExtensionsList() ) );

        return agentCapabilities.build();
    }

    protected List<AgentExtension> agentExtensionListToAgentExtensionList(List<org.a2aproject.sdk.grpc.AgentExtension> list) {
        if ( list == null ) {
            return null;
        }

        List<AgentExtension> list1 = new ArrayList<AgentExtension>( list.size() );
        for ( org.a2aproject.sdk.grpc.AgentExtension agentExtension : list ) {
            list1.add( agentExtensionMapper.fromProto( agentExtension ) );
        }

        return list1;
    }
}
