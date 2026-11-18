package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class ListTaskPushNotificationConfigsParamsMapperImpl implements ListTaskPushNotificationConfigsParamsMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public ListTaskPushNotificationConfigsParams fromProto(ListTaskPushNotificationConfigsRequest proto) {
        if ( proto == null ) {
            return null;
        }

        ListTaskPushNotificationConfigsParams.Builder listTaskPushNotificationConfigsParams = ListTaskPushNotificationConfigsParams.builder();

        listTaskPushNotificationConfigsParams.id( proto.getTaskId() );
        listTaskPushNotificationConfigsParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );
        listTaskPushNotificationConfigsParams.pageSize( proto.getPageSize() );
        listTaskPushNotificationConfigsParams.pageToken( proto.getPageToken() );

        return listTaskPushNotificationConfigsParams.build();
    }

    @Override
    public ListTaskPushNotificationConfigsRequest toProto(ListTaskPushNotificationConfigsParams domain) {
        if ( domain == null ) {
            return null;
        }

        ListTaskPushNotificationConfigsRequest.Builder listTaskPushNotificationConfigsRequest = ListTaskPushNotificationConfigsRequest.newBuilder();

        listTaskPushNotificationConfigsRequest.setTaskId( domain.id() );
        if ( domain.tenant() != null ) {
            listTaskPushNotificationConfigsRequest.setTenant( domain.tenant() );
        }
        listTaskPushNotificationConfigsRequest.setPageSize( domain.pageSize() );
        listTaskPushNotificationConfigsRequest.setPageToken( domain.pageToken() );

        return listTaskPushNotificationConfigsRequest.build();
    }
}
