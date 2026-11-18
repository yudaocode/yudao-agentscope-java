package org.a2aproject.sdk.grpc.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.ListTasksResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.Task;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class ListTasksResultMapperImpl implements ListTasksResultMapper {

    private final TaskMapper taskMapper = TaskMapper.INSTANCE;

    @Override
    public ListTasksResponse toProto(ListTasksResult domain) {
        if ( domain == null ) {
            return null;
        }

        ListTasksResponse.Builder listTasksResponse = ListTasksResponse.newBuilder();

        if ( domain.nextPageToken() != null ) {
            listTasksResponse.setNextPageToken( domain.nextPageToken() );
        }
        listTasksResponse.setPageSize( domain.pageSize() );
        listTasksResponse.setTotalSize( domain.totalSize() );
        if ( domain.tasks() != null ) {
            for ( Task task : domain.tasks() ) {
                listTasksResponse.addTasks( taskMapper.toProto( task ) );
            }
        }

        return listTasksResponse.build();
    }

    @Override
    public ListTasksResult fromProto(ListTasksResponse proto) {
        if ( proto == null ) {
            return null;
        }

        ListTasksResult.Builder listTasksResult = ListTasksResult.builder();

        if ( proto.getNextPageToken() != null ) {
            listTasksResult.nextPageToken( proto.getNextPageToken() );
        }
        listTasksResult.tasks( taskListToTaskList( proto.getTasksList() ) );
        listTasksResult.totalSize( proto.getTotalSize() );
        listTasksResult.pageSize( proto.getPageSize() );

        return listTasksResult.build();
    }

    protected List<Task> taskListToTaskList(List<org.a2aproject.sdk.grpc.Task> list) {
        if ( list == null ) {
            return null;
        }

        List<Task> list1 = new ArrayList<Task>( list.size() );
        for ( org.a2aproject.sdk.grpc.Task task : list ) {
            list1.add( taskMapper.fromProto( task ) );
        }

        return list1;
    }
}
