package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.AgentCardSignature;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentCardSignatureMapperImpl implements AgentCardSignatureMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.AgentCardSignature toProto(AgentCardSignature domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.AgentCardSignature.Builder agentCardSignature = org.a2aproject.sdk.grpc.AgentCardSignature.newBuilder();

        agentCardSignature.setProtected( domain.protectedHeader() );
        if ( domain.header() != null ) {
            agentCardSignature.setHeader( a2ACommonFieldMapper.mapToStruct( domain.header() ) );
        }
        agentCardSignature.setSignature( domain.signature() );

        return agentCardSignature.build();
    }

    @Override
    public AgentCardSignature fromProto(org.a2aproject.sdk.grpc.AgentCardSignature proto) {
        if ( proto == null ) {
            return null;
        }

        AgentCardSignature.Builder agentCardSignature = AgentCardSignature.builder();

        agentCardSignature.protectedHeader( proto.getProtected() );
        if ( proto.hasHeader() ) {
            agentCardSignature.header( a2ACommonFieldMapper.structToMap( proto.getHeader() ) );
        }
        agentCardSignature.signature( proto.getSignature() );

        return agentCardSignature.build();
    }
}
