package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskStatusUpdateEventMapperImpl implements TaskStatusUpdateEventMapper {

    private final TaskStatusMapper taskStatusMapper = TaskStatusMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.TaskStatusUpdateEvent toProto(TaskStatusUpdateEvent domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.TaskStatusUpdateEvent.Builder taskStatusUpdateEvent = org.a2aproject.sdk.grpc.TaskStatusUpdateEvent.newBuilder();

        taskStatusUpdateEvent.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        taskStatusUpdateEvent.setTaskId( domain.taskId() );
        taskStatusUpdateEvent.setContextId( domain.contextId() );
        taskStatusUpdateEvent.setStatus( taskStatusMapper.toProto( domain.status() ) );

        return taskStatusUpdateEvent.build();
    }
}
