package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AgentExtension;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentExtensionMapperImpl implements AgentExtensionMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.AgentExtension toProto(AgentExtension domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AgentExtension.Builder agentExtension = org.a2aproject.sdk.grpc.AgentExtension.newBuilder();

        if ( domain.params() != null ) {
            agentExtension.setParams( a2ACommonFieldMapper.mapToStruct( domain.params() ) );
        }
        agentExtension.setUri( domain.uri() );
        agentExtension.setDescription( domain.description() );
        agentExtension.setRequired( domain.required() );

        return agentExtension.build();
    }

    @Override
    public AgentExtension fromProto(org.a2aproject.sdk.grpc.AgentExtension proto) {
        if ( proto == null ) {
            return null;
        }

        AgentExtension.Builder agentExtension = AgentExtension.builder();

        if ( proto.hasParams() ) {
            agentExtension.params( a2ACommonFieldMapper.structToMap( proto.getParams() ) );
        }
        agentExtension.description( proto.getDescription() );
        agentExtension.required( proto.getRequired() );
        agentExtension.uri( proto.getUri() );

        return agentExtension.build();
    }
}
