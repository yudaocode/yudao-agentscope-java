package org.a2aproject.sdk.grpc.mapper;

import java.util.List;
import javax.annotation.Generated;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class MessageMapperImpl implements MessageMapper {

    private final RoleMapper roleMapper = RoleMapper.INSTANCE;
    private final PartMapper partMapper = PartMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.Message toProto(Message domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.Message.Builder message = org.a2aproject.sdk.grpc.Message.newBuilder();

        if ( domain.messageId() != null ) {
            message.setMessageId( domain.messageId() );
        }
        if ( domain.contextId() != null ) {
            message.setContextId( domain.contextId() );
        }
        if ( domain.taskId() != null ) {
            message.setTaskId( domain.taskId() );
        }
        message.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        message.setRole( roleMapper.toProto( domain.role() ) );
        if ( domain.parts() != null ) {
            for ( Part<?> part : domain.parts() ) {
                message.addParts( partMapper.toProto( part ) );
            }
        }
        if ( domain.extensions() != null ) {
            for ( String extension : domain.extensions() ) {
                message.addExtensions( extension );
            }
        }
        if ( domain.referenceTaskIds() != null ) {
            for ( String referenceTaskId : domain.referenceTaskIds() ) {
                message.addReferenceTaskIds( referenceTaskId );
            }
        }

        return message.build();
    }

    @Override
    public Message fromProto(org.a2aproject.sdk.grpc.Message proto) {
        if ( proto == null ) {
            return null;
        }

        Message.Builder message = Message.builder();

        message.messageId( a2ACommonFieldMapper.requireNonEmpty( proto.getMessageId() ) );
        message.contextId( a2ACommonFieldMapper.emptyToNull( proto.getContextId() ) );
        message.taskId( a2ACommonFieldMapper.emptyToNull( proto.getTaskId() ) );
        if ( proto.hasMetadata() ) {
            message.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        message.role( roleMapper.fromProto( proto.getRole() ) );
        message.parts( partListToPartArray( proto.getPartsList() ) );

        message.extensions( org.a2aproject.sdk.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getExtensionsList()) );
        message.referenceTaskIds( org.a2aproject.sdk.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getReferenceTaskIdsList()) );

        return message.build();
    }

    protected Part[] partListToPartArray(List<org.a2aproject.sdk.grpc.Part> list) {
        if ( list == null ) {
            return null;
        }

        Part<?>[] partTmp = new Part<?>[list.size()];
        int i = 0;
        for ( org.a2aproject.sdk.grpc.Part part : list ) {
            partTmp[i] = partMapper.fromProto( part );
            i++;
        }

        return partTmp;
    }
}
