package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.SendMessageConfiguration;
import org.a2aproject.sdk.spec.MessageSendConfiguration;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class MessageSendConfigurationMapperImpl implements MessageSendConfigurationMapper {

    private final TaskPushNotificationConfigMapper taskPushNotificationConfigMapper = TaskPushNotificationConfigMapper.INSTANCE;

    @Override
    public SendMessageConfiguration toProto(MessageSendConfiguration domain) {
        if ( domain == null ) {
            return null;
        }

        SendMessageConfiguration.Builder sendMessageConfiguration = SendMessageConfiguration.newBuilder();

        if ( domain.taskPushNotificationConfig() != null ) {
            sendMessageConfiguration.setTaskPushNotificationConfig( taskPushNotificationConfigMapper.toProto( domain.taskPushNotificationConfig() ) );
        }
        if ( domain.historyLength() != null ) {
            sendMessageConfiguration.setHistoryLength( domain.historyLength() );
        }
        if ( domain.returnImmediately() != null ) {
            sendMessageConfiguration.setReturnImmediately( domain.returnImmediately() );
        }
        if ( domain.acceptedOutputModes() != null ) {
            for ( String acceptedOutputMode : domain.acceptedOutputModes() ) {
                sendMessageConfiguration.addAcceptedOutputModes( acceptedOutputMode );
            }
        }

        return sendMessageConfiguration.build();
    }

    @Override
    public MessageSendConfiguration fromProto(SendMessageConfiguration proto) {
        if ( proto == null ) {
            return null;
        }

        MessageSendConfiguration.Builder messageSendConfiguration = MessageSendConfiguration.builder();

        if ( proto.hasTaskPushNotificationConfig() ) {
            messageSendConfiguration.taskPushNotificationConfig( taskPushNotificationConfigMapper.fromProto( proto.getTaskPushNotificationConfig() ) );
        }
        if ( proto.hasHistoryLength() ) {
            messageSendConfiguration.historyLength( proto.getHistoryLength() );
        }
        messageSendConfiguration.returnImmediately( proto.getReturnImmediately() );

        messageSendConfiguration.acceptedOutputModes( org.a2aproject.sdk.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getAcceptedOutputModesList()) );

        return messageSendConfiguration.build();
    }
}
