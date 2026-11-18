package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskPushNotificationConfigMapperImpl implements TaskPushNotificationConfigMapper {

    private final AuthenticationInfoMapper authenticationInfoMapper = AuthenticationInfoMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.TaskPushNotificationConfig toProto(TaskPushNotificationConfig domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder taskPushNotificationConfig = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();

        if ( domain.id() != null ) {
            taskPushNotificationConfig.setId( domain.id() );
        }
        if ( domain.taskId() != null ) {
            taskPushNotificationConfig.setTaskId( domain.taskId() );
        }
        if ( domain.url() != null ) {
            taskPushNotificationConfig.setUrl( domain.url() );
        }
        if ( domain.token() != null ) {
            taskPushNotificationConfig.setToken( domain.token() );
        }
        if ( domain.tenant() != null ) {
            taskPushNotificationConfig.setTenant( domain.tenant() );
        }
        if ( domain.authentication() != null ) {
            taskPushNotificationConfig.setAuthentication( authenticationInfoMapper.toProto( domain.authentication() ) );
        }

        return taskPushNotificationConfig.build();
    }

    @Override
    public TaskPushNotificationConfig fromProto(org.a2aproject.sdk.grpc.TaskPushNotificationConfig proto) {
        if ( proto == null ) {
            return null;
        }

        TaskPushNotificationConfig.Builder taskPushNotificationConfig = TaskPushNotificationConfig.builder();

        taskPushNotificationConfig.token( a2ACommonFieldMapper.emptyToNull( proto.getToken() ) );
        taskPushNotificationConfig.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );
        taskPushNotificationConfig.taskId( a2ACommonFieldMapper.emptyToNull( proto.getTaskId() ) );
        if ( proto.hasAuthentication() ) {
            taskPushNotificationConfig.authentication( authenticationInfoMapper.fromProto( proto.getAuthentication() ) );
        }
        taskPushNotificationConfig.id( proto.getId() );
        taskPushNotificationConfig.url( proto.getUrl() );

        return taskPushNotificationConfig.build();
    }
}
