package org.a2aproject.sdk.grpc.mapper;

import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.ListTasksRequest;
import org.a2aproject.sdk.spec.ListTasksParams;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class ListTasksParamsMapperImpl implements ListTasksParamsMapper {

    private final TaskStateMapper taskStateMapper = TaskStateMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public ListTasksRequest toProto(ListTasksParams params) {
        if ( params == null ) {
            return null;
        }

        ListTasksRequest.Builder listTasksRequest = ListTasksRequest.newBuilder();

        if ( params.contextId() != null ) {
            listTasksRequest.setContextId( params.contextId() );
        }
        if ( params.status() != null ) {
            listTasksRequest.setStatus( taskStateMapper.toProto( params.status() ) );
        }
        if ( params.pageSize() != null ) {
            listTasksRequest.setPageSize( params.pageSize() );
        }
        if ( params.pageToken() != null ) {
            listTasksRequest.setPageToken( params.pageToken() );
        }
        if ( params.historyLength() != null ) {
            listTasksRequest.setHistoryLength( params.historyLength() );
        }
        listTasksRequest.setStatusTimestampAfter( a2ACommonFieldMapper.instantToProtoTimestamp( params.statusTimestampAfter() ) );
        if ( params.includeArtifacts() != null ) {
            listTasksRequest.setIncludeArtifacts( params.includeArtifacts() );
        }
        if ( params.tenant() != null ) {
            listTasksRequest.setTenant( params.tenant() );
        }

        return listTasksRequest.build();
    }

    @Override
    public ListTasksParams fromProto(ListTasksRequest request) {
        if ( request == null ) {
            return null;
        }

        ListTasksParams.Builder listTasksParams = ListTasksParams.builder();

        listTasksParams.contextId( a2ACommonFieldMapper.emptyToNull( request.getContextId() ) );
        listTasksParams.status( a2ACommonFieldMapper.taskStateOrNull( request.getStatus() ) );
        listTasksParams.pageToken( a2ACommonFieldMapper.emptyToNull( request.getPageToken() ) );
        if ( request.hasStatusTimestampAfter() ) {
            listTasksParams.statusTimestampAfter( a2ACommonFieldMapper.protoTimestampToInstant( request.getStatusTimestampAfter() ) );
        }
        if ( request.hasIncludeArtifacts() ) {
            listTasksParams.includeArtifacts( a2ACommonFieldMapper.falseToNull( request.getIncludeArtifacts() ) );
        }
        listTasksParams.tenant( a2ACommonFieldMapper.emptyToNull( request.getTenant() ) );

        listTasksParams.pageSize( request.hasPageSize() ? request.getPageSize() : null );
        listTasksParams.historyLength( request.hasHistoryLength() ? request.getHistoryLength() : null );

        return listTasksParams.build();
    }
}
