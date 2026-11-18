package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.CancelTaskRequest;
import org.a2aproject.sdk.grpc.SubscribeToTaskRequest;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.TaskIdParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskIdParamsMapperImpl implements TaskIdParamsMapper {

    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public CancelTaskParams fromProtoCancelTaskRequest(CancelTaskRequest proto) {
        if ( proto == null ) {
            return null;
        }

        CancelTaskParams.Builder cancelTaskParams = CancelTaskParams.builder();

        cancelTaskParams.id( proto.getId() );
        if ( proto.hasMetadata() ) {
            cancelTaskParams.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        cancelTaskParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );

        return cancelTaskParams.build();
    }

    @Override
    public CancelTaskRequest toProtoCancelTaskRequest(CancelTaskParams domain) {
        if ( domain == null ) {
            return null;
        }

        CancelTaskRequest.Builder cancelTaskRequest = CancelTaskRequest.newBuilder();

        cancelTaskRequest.setId( domain.id() );
        cancelTaskRequest.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        if ( domain.tenant() != null ) {
            cancelTaskRequest.setTenant( domain.tenant() );
        }

        return cancelTaskRequest.build();
    }

    @Override
    public TaskIdParams fromProtoSubscribeToTaskRequest(SubscribeToTaskRequest proto) {
        if ( proto == null ) {
            return null;
        }

        TaskIdParams.Builder taskIdParams = TaskIdParams.builder();

        taskIdParams.id( proto.getId() );
        taskIdParams.tenant( a2ACommonFieldMapper.emptyToNull( proto.getTenant() ) );

        return taskIdParams.build();
    }

    @Override
    public SubscribeToTaskRequest toProtoSubscribeToTaskRequest(TaskIdParams domain) {
        if ( domain == null ) {
            return null;
        }

        SubscribeToTaskRequest.Builder subscribeToTaskRequest = SubscribeToTaskRequest.newBuilder();

        subscribeToTaskRequest.setId( domain.id() );
        if ( domain.tenant() != null ) {
            subscribeToTaskRequest.setTenant( domain.tenant() );
        }

        return subscribeToTaskRequest.build();
    }
}
