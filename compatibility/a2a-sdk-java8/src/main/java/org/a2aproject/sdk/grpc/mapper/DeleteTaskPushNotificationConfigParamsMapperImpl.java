package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class DeleteTaskPushNotificationConfigParamsMapperImpl implements DeleteTaskPushNotificationConfigParamsMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public DeleteTaskPushNotificationConfigParams fromProto(DeleteTaskPushNotificationConfigRequest proto) {
        if ( proto == null ) {
            return null;
        }

        DeleteTaskPushNotificationConfigParams.Builder deleteTaskPushNotificationConfigParams = DeleteTaskPushNotificationConfigParams.builder();

        deleteTaskPushNotificationConfigParams.taskId( proto.getTaskId() );
        deleteTaskPushNotificationConfigParams.id( proto.getId() );
        deleteTaskPushNotificationConfigParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );

        return deleteTaskPushNotificationConfigParams.build();
    }

    @Override
    public DeleteTaskPushNotificationConfigRequest toProto(DeleteTaskPushNotificationConfigParams domain) {
        if ( domain == null ) {
            return null;
        }

        DeleteTaskPushNotificationConfigRequest.Builder deleteTaskPushNotificationConfigRequest = DeleteTaskPushNotificationConfigRequest.newBuilder();

        deleteTaskPushNotificationConfigRequest.setTaskId( domain.taskId() );
        deleteTaskPushNotificationConfigRequest.setId( domain.id() );
        if ( domain.tenant() != null ) {
            deleteTaskPushNotificationConfigRequest.setTenant( domain.tenant() );
        }

        return deleteTaskPushNotificationConfigRequest.build();
    }
}
