package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class GetTaskPushNotificationConfigParamsMapperImpl implements GetTaskPushNotificationConfigParamsMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public GetTaskPushNotificationConfigParams fromProto(GetTaskPushNotificationConfigRequest proto) {
        if ( proto == null ) {
            return null;
        }

        GetTaskPushNotificationConfigParams.Builder getTaskPushNotificationConfigParams = GetTaskPushNotificationConfigParams.builder();

        getTaskPushNotificationConfigParams.taskId( proto.getTaskId() );
        getTaskPushNotificationConfigParams.id( proto.getId() );
        getTaskPushNotificationConfigParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );

        return getTaskPushNotificationConfigParams.build();
    }

    @Override
    public GetTaskPushNotificationConfigRequest toProto(GetTaskPushNotificationConfigParams domain) {
        if ( domain == null ) {
            return null;
        }

        GetTaskPushNotificationConfigRequest.Builder getTaskPushNotificationConfigRequest = GetTaskPushNotificationConfigRequest.newBuilder();

        getTaskPushNotificationConfigRequest.setTaskId( domain.taskId() );
        if ( domain.id() != null ) {
            getTaskPushNotificationConfigRequest.setId( domain.id() );
        }
        if ( domain.tenant() != null ) {
            getTaskPushNotificationConfigRequest.setTenant( domain.tenant() );
        }

        return getTaskPushNotificationConfigRequest.build();
    }
}
