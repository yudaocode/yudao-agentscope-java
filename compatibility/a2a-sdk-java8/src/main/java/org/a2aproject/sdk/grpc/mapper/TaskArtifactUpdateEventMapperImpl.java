package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskArtifactUpdateEventMapperImpl implements TaskArtifactUpdateEventMapper {

    private final ArtifactMapper artifactMapper = ArtifactMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent toProto(TaskArtifactUpdateEvent domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent.Builder taskArtifactUpdateEvent = org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent.newBuilder();

        if ( domain.append() != null ) {
            taskArtifactUpdateEvent.setAppend( domain.append() );
        }
        if ( domain.lastChunk() != null ) {
            taskArtifactUpdateEvent.setLastChunk( domain.lastChunk() );
        }
        taskArtifactUpdateEvent.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        taskArtifactUpdateEvent.setTaskId( domain.taskId() );
        taskArtifactUpdateEvent.setContextId( domain.contextId() );
        taskArtifactUpdateEvent.setArtifact( artifactMapper.toProto( domain.artifact() ) );

        return taskArtifactUpdateEvent.build();
    }

    @Override
    public TaskArtifactUpdateEvent fromProto(org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent proto) {
        if ( proto == null ) {
            return null;
        }

        TaskArtifactUpdateEvent.Builder taskArtifactUpdateEvent = TaskArtifactUpdateEvent.builder();

        if ( proto.hasMetadata() ) {
            taskArtifactUpdateEvent.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        taskArtifactUpdateEvent.taskId( proto.getTaskId() );
        if ( proto.hasArtifact() ) {
            taskArtifactUpdateEvent.artifact( artifactMapper.fromProto( proto.getArtifact() ) );
        }
        taskArtifactUpdateEvent.contextId( proto.getContextId() );
        taskArtifactUpdateEvent.append( proto.getAppend() );
        taskArtifactUpdateEvent.lastChunk( proto.getLastChunk() );

        return taskArtifactUpdateEvent.build();
    }
}
