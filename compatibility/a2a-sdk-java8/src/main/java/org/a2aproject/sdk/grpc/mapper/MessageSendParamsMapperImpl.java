package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.SendMessageRequest;
import org.a2aproject.sdk.spec.MessageSendParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class MessageSendParamsMapperImpl implements MessageSendParamsMapper {

    private final MessageMapper messageMapper = MessageMapper.INSTANCE;
    private final MessageSendConfigurationMapper messageSendConfigurationMapper = MessageSendConfigurationMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public SendMessageRequest toProto(MessageSendParams domain) {
        if ( domain == null ) {
            return null;
        }

        SendMessageRequest.Builder sendMessageRequest = SendMessageRequest.newBuilder();

        if ( domain.configuration() != null ) {
            sendMessageRequest.setConfiguration( messageSendConfigurationMapper.toProto( domain.configuration() ) );
        }
        sendMessageRequest.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        if ( domain.tenant() != null ) {
            sendMessageRequest.setTenant( domain.tenant() );
        }
        sendMessageRequest.setMessage( messageMapper.toProto( domain.message() ) );

        return sendMessageRequest.build();
    }

    @Override
    public MessageSendParams fromProto(SendMessageRequest proto) {
        if ( proto == null ) {
            return null;
        }

        MessageSendParams.Builder messageSendParams = MessageSendParams.builder();

        if ( proto.hasMetadata() ) {
            messageSendParams.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        messageSendParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );
        if ( proto.hasMessage() ) {
            messageSendParams.message( messageMapper.fromProto( proto.getMessage() ) );
        }
        if ( proto.hasConfiguration() ) {
            messageSendParams.configuration( messageSendConfigurationMapper.fromProto( proto.getConfiguration() ) );
        }

        return messageSendParams.build();
    }
}
