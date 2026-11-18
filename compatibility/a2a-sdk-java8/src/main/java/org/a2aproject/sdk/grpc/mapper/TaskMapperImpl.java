package org.a2aproject.sdk.grpc.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskMapperImpl implements TaskMapper {

    private final TaskStatusMapper taskStatusMapper = TaskStatusMapper.INSTANCE;
    private final ArtifactMapper artifactMapper = ArtifactMapper.INSTANCE;
    private final MessageMapper messageMapper = MessageMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.Task toProto(Task domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.Task.Builder task = org.a2aproject.sdk.grpc.Task.newBuilder();

        if ( domain.id() != null ) {
            task.setId( domain.id() );
        }
        if ( domain.contextId() != null ) {
            task.setContextId( domain.contextId() );
        }
        task.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        task.setStatus( taskStatusMapper.toProto( domain.status() ) );
        if ( domain.artifacts() != null ) {
            for ( Artifact artifact : domain.artifacts() ) {
                task.addArtifacts( artifactMapper.toProto( artifact ) );
            }
        }
        if ( domain.history() != null ) {
            for ( Message history : domain.history() ) {
                task.addHistory( messageMapper.toProto( history ) );
            }
        }

        return task.build();
    }

    @Override
    public Task fromProto(org.a2aproject.sdk.grpc.Task proto) {
        if ( proto == null ) {
            return null;
        }

        Task.Builder task = Task.builder();

        task.id( a2ACommonFieldMapper.emptyToNull( proto.getId() ) );
        task.contextId( a2ACommonFieldMapper.emptyToNull( proto.getContextId() ) );
        if ( proto.hasMetadata() ) {
            task.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        if ( proto.hasStatus() ) {
            task.status( taskStatusMapper.fromProto( proto.getStatus() ) );
        }
        task.artifacts( artifactListToArtifactList( proto.getArtifactsList() ) );
        task.history( messageListToMessageArray( proto.getHistoryList() ) );

        return task.build();
    }

    protected List<Artifact> artifactListToArtifactList(List<org.a2aproject.sdk.grpc.Artifact> list) {
        if ( list == null ) {
            return null;
        }

        List<Artifact> list1 = new ArrayList<Artifact>( list.size() );
        for ( org.a2aproject.sdk.grpc.Artifact artifact : list ) {
            list1.add( artifactMapper.fromProto( artifact ) );
        }

        return list1;
    }

    protected Message[] messageListToMessageArray(List<org.a2aproject.sdk.grpc.Message> list) {
        if ( list == null ) {
            return null;
        }

        Message[] messageTmp = new Message[list.size()];
        int i = 0;
        for ( org.a2aproject.sdk.grpc.Message message : list ) {
            messageTmp[i] = messageMapper.fromProto( message );
            i++;
        }

        return messageTmp;
    }
}
