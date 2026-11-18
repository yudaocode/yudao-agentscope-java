package org.a2aproject.sdk.grpc.mapper;

import com.google.protobuf.ProtocolStringList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.AgentCard;
import org.a2aproject.sdk.grpc.SecurityScheme;
import org.a2aproject.sdk.spec.AgentCardSignature;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.SecurityRequirement;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class AgentCardMapperImpl implements AgentCardMapper {

    private final AgentProviderMapper agentProviderMapper = AgentProviderMapper.INSTANCE;
    private final AgentCapabilitiesMapper agentCapabilitiesMapper = AgentCapabilitiesMapper.INSTANCE;
    private final AgentSkillMapper agentSkillMapper = AgentSkillMapper.INSTANCE;
    private final SecuritySchemeMapper securitySchemeMapper = SecuritySchemeMapper.INSTANCE;
    private final SecurityRequirementMapper securityRequirementMapper = SecurityRequirementMapper.INSTANCE;
    private final AgentInterfaceMapper agentInterfaceMapper = AgentInterfaceMapper.INSTANCE;
    private final AgentCardSignatureMapper agentCardSignatureMapper = AgentCardSignatureMapper.INSTANCE;

    @Override
    public AgentCard toProto(org.a2aproject.sdk.spec.AgentCard domain) {
        if ( domain == null ) {
            return null;
        }

        AgentCard.Builder agentCard = AgentCard.newBuilder();

        if ( domain.provider() != null ) {
            agentCard.setProvider( agentProviderMapper.toProto( domain.provider() ) );
        }
        if ( domain.documentationUrl() != null ) {
            agentCard.setDocumentationUrl( domain.documentationUrl() );
        }
        if ( domain.iconUrl() != null ) {
            agentCard.setIconUrl( domain.iconUrl() );
        }
        agentCard.setName( domain.name() );
        agentCard.setDescription( domain.description() );
        agentCard.setVersion( domain.version() );
        agentCard.setCapabilities( agentCapabilitiesMapper.toProto( domain.capabilities() ) );
        if ( domain.supportedInterfaces() != null ) {
            for ( AgentInterface supportedInterface : domain.supportedInterfaces() ) {
                agentCard.addSupportedInterfaces( agentInterfaceMapper.toProto( supportedInterface ) );
            }
        }
        if ( agentCard.getMutableSecuritySchemes() != null ) {
            Map<String, SecurityScheme> map = stringSecuritySchemeMapToStringSecuritySchemeMap( domain.securitySchemes() );
            if ( map != null ) {
                agentCard.getMutableSecuritySchemes().putAll( map );
            }
        }
        if ( domain.securityRequirements() != null ) {
            for ( SecurityRequirement securityRequirement : domain.securityRequirements() ) {
                agentCard.addSecurityRequirements( securityRequirementMapper.mapSecurityRequirement( securityRequirement ) );
            }
        }
        if ( domain.defaultInputModes() != null ) {
            for ( String defaultInputMode : domain.defaultInputModes() ) {
                agentCard.addDefaultInputModes( defaultInputMode );
            }
        }
        if ( domain.defaultOutputModes() != null ) {
            for ( String defaultOutputMode : domain.defaultOutputModes() ) {
                agentCard.addDefaultOutputModes( defaultOutputMode );
            }
        }
        if ( domain.skills() != null ) {
            for ( AgentSkill skill : domain.skills() ) {
                agentCard.addSkills( agentSkillMapper.toProto( skill ) );
            }
        }
        if ( domain.signatures() != null ) {
            for ( AgentCardSignature signature : domain.signatures() ) {
                agentCard.addSignatures( agentCardSignatureMapper.toProto( signature ) );
            }
        }

        return agentCard.build();
    }

    @Override
    public org.a2aproject.sdk.spec.AgentCard fromProto(AgentCard proto) {
        if ( proto == null ) {
            return null;
        }

        org.a2aproject.sdk.spec.AgentCard.Builder agentCard = org.a2aproject.sdk.spec.AgentCard.builder();

        if ( proto.hasProvider() ) {
            agentCard.provider( agentProviderMapper.fromProto( proto.getProvider() ) );
        }
        if ( !proto.getDocumentationUrl().isEmpty() ) {
            agentCard.documentationUrl( proto.getDocumentationUrl() );
        }
        if ( !proto.getIconUrl().isEmpty() ) {
            agentCard.iconUrl( proto.getIconUrl() );
        }
        agentCard.name( proto.getName() );
        agentCard.description( proto.getDescription() );
        agentCard.version( proto.getVersion() );
        if ( proto.hasCapabilities() ) {
            agentCard.capabilities( agentCapabilitiesMapper.fromProto( proto.getCapabilities() ) );
        }
        ProtocolStringList protocolStringList = proto.getDefaultInputModesList();
        if ( protocolStringList != null ) {
            agentCard.defaultInputModes( new ArrayList<String>( protocolStringList ) );
        }
        ProtocolStringList protocolStringList1 = proto.getDefaultOutputModesList();
        if ( protocolStringList1 != null ) {
            agentCard.defaultOutputModes( new ArrayList<String>( protocolStringList1 ) );
        }
        agentCard.skills( agentSkillListToAgentSkillList( proto.getSkillsList() ) );
        agentCard.securitySchemes( stringSecuritySchemeMapToStringSecuritySchemeMap1( proto.getSecuritySchemes() ) );
        agentCard.securityRequirements( securityRequirementMapper.fromProto( proto.getSecurityRequirementsList() ) );
        agentCard.supportedInterfaces( agentInterfaceListToAgentInterfaceList( proto.getSupportedInterfacesList() ) );
        agentCard.signatures( agentCardSignatureListToAgentCardSignatureList( proto.getSignaturesList() ) );

        return agentCard.build();
    }

    protected Map<String, SecurityScheme> stringSecuritySchemeMapToStringSecuritySchemeMap(Map<String, org.a2aproject.sdk.spec.SecurityScheme> map) {
        if ( map == null ) {
            return null;
        }

        Map<String, SecurityScheme> map1 = new LinkedHashMap<String, SecurityScheme>( Math.max( (int) ( map.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, org.a2aproject.sdk.spec.SecurityScheme> entry : map.entrySet() ) {
            String key = entry.getKey();
            SecurityScheme value = securitySchemeMapper.toProto( entry.getValue() );
            map1.put( key, value );
        }

        return map1;
    }

    protected List<AgentSkill> agentSkillListToAgentSkillList(List<org.a2aproject.sdk.grpc.AgentSkill> list) {
        if ( list == null ) {
            return null;
        }

        List<AgentSkill> list1 = new ArrayList<AgentSkill>( list.size() );
        for ( org.a2aproject.sdk.grpc.AgentSkill agentSkill : list ) {
            list1.add( agentSkillMapper.fromProto( agentSkill ) );
        }

        return list1;
    }

    protected Map<String, org.a2aproject.sdk.spec.SecurityScheme> stringSecuritySchemeMapToStringSecuritySchemeMap1(Map<String, SecurityScheme> map) {
        if ( map == null ) {
            return null;
        }

        Map<String, org.a2aproject.sdk.spec.SecurityScheme> map1 = new LinkedHashMap<String, org.a2aproject.sdk.spec.SecurityScheme>( Math.max( (int) ( map.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, SecurityScheme> entry : map.entrySet() ) {
            String key = entry.getKey();
            org.a2aproject.sdk.spec.SecurityScheme value = securitySchemeMapper.fromProto( entry.getValue() );
            map1.put( key, value );
        }

        return map1;
    }

    protected List<AgentInterface> agentInterfaceListToAgentInterfaceList(List<org.a2aproject.sdk.grpc.AgentInterface> list) {
        if ( list == null ) {
            return null;
        }

        List<AgentInterface> list1 = new ArrayList<AgentInterface>( list.size() );
        for ( org.a2aproject.sdk.grpc.AgentInterface agentInterface : list ) {
            list1.add( agentInterfaceMapper.fromProto( agentInterface ) );
        }

        return list1;
    }

    protected List<AgentCardSignature> agentCardSignatureListToAgentCardSignatureList(List<org.a2aproject.sdk.grpc.AgentCardSignature> list) {
        if ( list == null ) {
            return null;
        }

        List<AgentCardSignature> list1 = new ArrayList<AgentCardSignature>( list.size() );
        for ( org.a2aproject.sdk.grpc.AgentCardSignature agentCardSignature : list ) {
            list1.add( agentCardSignatureMapper.fromProto( agentCardSignature ) );
        }

        return list1;
    }
}
