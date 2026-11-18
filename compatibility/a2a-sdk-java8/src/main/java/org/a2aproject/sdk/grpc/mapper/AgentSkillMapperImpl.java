package org.a2aproject.sdk.grpc.mapper;

import com.google.protobuf.ProtocolStringList;
import java.util.ArrayList;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.AgentSkill;
import org.a2aproject.sdk.spec.SecurityRequirement;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentSkillMapperImpl implements AgentSkillMapper {

    private final SecurityRequirementMapper securityRequirementMapper = SecurityRequirementMapper.INSTANCE;

    @Override
    public AgentSkill toProto(org.a2aproject.sdk.spec.AgentSkill domain) {
        if ( domain == null ) {
            return null;
        }

        AgentSkill.Builder agentSkill = AgentSkill.newBuilder();

        agentSkill.setId( domain.id() );
        agentSkill.setName( domain.name() );
        agentSkill.setDescription( domain.description() );
        if ( domain.tags() != null ) {
            for ( String tag : domain.tags() ) {
                agentSkill.addTags( tag );
            }
        }
        if ( domain.examples() != null ) {
            for ( String example : domain.examples() ) {
                agentSkill.addExamples( example );
            }
        }
        if ( domain.inputModes() != null ) {
            for ( String inputMode : domain.inputModes() ) {
                agentSkill.addInputModes( inputMode );
            }
        }
        if ( domain.outputModes() != null ) {
            for ( String outputMode : domain.outputModes() ) {
                agentSkill.addOutputModes( outputMode );
            }
        }
        if ( domain.securityRequirements() != null ) {
            for ( SecurityRequirement securityRequirement : domain.securityRequirements() ) {
                agentSkill.addSecurityRequirements( securityRequirementMapper.mapSecurityRequirement( securityRequirement ) );
            }
        }

        return agentSkill.build();
    }

    @Override
    public org.a2aproject.sdk.spec.AgentSkill fromProto(AgentSkill proto) {
        if ( proto == null ) {
            return null;
        }

        org.a2aproject.sdk.spec.AgentSkill.Builder agentSkill = org.a2aproject.sdk.spec.AgentSkill.builder();

        agentSkill.id( proto.getId() );
        agentSkill.name( proto.getName() );
        agentSkill.description( proto.getDescription() );
        ProtocolStringList protocolStringList = proto.getTagsList();
        if ( protocolStringList != null ) {
            agentSkill.tags( new ArrayList<String>( protocolStringList ) );
        }
        ProtocolStringList protocolStringList1 = proto.getExamplesList();
        if ( protocolStringList1 != null ) {
            agentSkill.examples( new ArrayList<String>( protocolStringList1 ) );
        }
        ProtocolStringList protocolStringList2 = proto.getInputModesList();
        if ( protocolStringList2 != null ) {
            agentSkill.inputModes( new ArrayList<String>( protocolStringList2 ) );
        }
        ProtocolStringList protocolStringList3 = proto.getOutputModesList();
        if ( protocolStringList3 != null ) {
            agentSkill.outputModes( new ArrayList<String>( protocolStringList3 ) );
        }
        agentSkill.securityRequirements( securityRequirementMapper.fromProto( proto.getSecurityRequirementsList() ) );

        return agentSkill.build();
    }
}
