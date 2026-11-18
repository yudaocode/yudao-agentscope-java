package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.GetTaskRequest;
import org.a2aproject.sdk.spec.TaskQueryParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskQueryParamsMapperImpl implements TaskQueryParamsMapper {

    @Override
    public TaskQueryParams fromProto(GetTaskRequest proto) {
        if ( proto == null ) {
            return null;
        }

        TaskQueryParams.Builder taskQueryParams = TaskQueryParams.builder();

        taskQueryParams.id( proto.getId() );
        if ( proto.hasHistoryLength() ) {
            taskQueryParams.historyLength( proto.getHistoryLength() );
        }
        taskQueryParams.tenant( proto.getTenant() );

        return taskQueryParams.build();
    }

    @Override
    public GetTaskRequest toProto(TaskQueryParams domain) {
        if ( domain == null ) {
            return null;
        }

        GetTaskRequest.Builder getTaskRequest = GetTaskRequest.newBuilder();

        getTaskRequest.setId( domain.id() );
        if ( domain.historyLength() != null ) {
            getTaskRequest.setHistoryLength( domain.historyLength() );
        }
        if ( domain.tenant() != null ) {
            getTaskRequest.setTenant( domain.tenant() );
        }

        return getTaskRequest.build();
    }
}
