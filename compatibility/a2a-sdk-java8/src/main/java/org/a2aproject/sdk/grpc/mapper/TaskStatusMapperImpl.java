package org.a2aproject.sdk.grpc.mapper;

import java.time.OffsetDateTime;
import javax.annotation.Generated;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class TaskStatusMapperImpl implements TaskStatusMapper {

    private final TaskStateMapper taskStateMapper = TaskStateMapper.INSTANCE;
    private final MessageMapper messageMapper = MessageMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public org.a2aproject.sdk.grpc.TaskStatus toProto(TaskStatus domain) {
        if ( domain == null ) {
            return null;
        }

        org.a2aproject.sdk.grpc.TaskStatus.Builder taskStatus = org.a2aproject.sdk.grpc.TaskStatus.newBuilder();

        if ( domain.state() != null ) {
            taskStatus.setState( taskStateMapper.toProto( domain.state() ) );
        }
        if ( domain.message() != null ) {
            taskStatus.setMessage( messageMapper.toProto( domain.message() ) );
        }
        taskStatus.setTimestamp( a2ACommonFieldMapper.offsetDateTimeToProtoTimestamp( domain.timestamp() ) );

        return taskStatus.build();
    }

    @Override
    public TaskStatus fromProto(org.a2aproject.sdk.grpc.TaskStatus proto) {
        if ( proto == null ) {
            return null;
        }

        OffsetDateTime timestamp = null;
        TaskState state = null;
        Message message = null;

        if ( proto.hasTimestamp() ) {
            timestamp = a2ACommonFieldMapper.protoTimestampToOffsetDateTime( proto.getTimestamp() );
        }
        state = taskStateMapper.fromProto( proto.getState() );
        if ( proto.hasMessage() ) {
            message = messageMapper.fromProto( proto.getMessage() );
        }

        TaskStatus taskStatus = new TaskStatus( state, message, timestamp );

        return taskStatus;
    }
}
