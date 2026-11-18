package org.a2aproject.sdk.grpc.mapper;

import java.util.List;
import javax.annotation.Generated;
import org.a2aproject.sdk.grpc.Artifact;
import org.a2aproject.sdk.spec.Part;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-09T14:36:41+0000",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.21 (Eclipse Adoptium)"
)
public class ArtifactMapperImpl implements ArtifactMapper {

    private final PartMapper partMapper = PartMapper.INSTANCE;
    private final A2ACommonFieldMapper a2ACommonFieldMapper = A2ACommonFieldMapper.INSTANCE;

    @Override
    public Artifact toProto(org.a2aproject.sdk.spec.Artifact domain) {
        if ( domain == null ) {
            return null;
        }

        Artifact.Builder artifact = Artifact.newBuilder();

        if ( domain.artifactId() != null ) {
            artifact.setArtifactId( domain.artifactId() );
        }
        if ( domain.name() != null ) {
            artifact.setName( domain.name() );
        }
        if ( domain.description() != null ) {
            artifact.setDescription( domain.description() );
        }
        artifact.setMetadata( a2ACommonFieldMapper.metadataToProto( domain.metadata() ) );
        if ( domain.parts() != null ) {
            for ( Part<?> part : domain.parts() ) {
                artifact.addParts( partMapper.toProto( part ) );
            }
        }
        if ( domain.extensions() != null ) {
            for ( String extension : domain.extensions() ) {
                artifact.addExtensions( extension );
            }
        }

        return artifact.build();
    }

    @Override
    public org.a2aproject.sdk.spec.Artifact fromProto(Artifact proto) {
        if ( proto == null ) {
            return null;
        }

        org.a2aproject.sdk.spec.Artifact.Builder artifact = org.a2aproject.sdk.spec.Artifact.builder();

        artifact.artifactId( a2ACommonFieldMapper.emptyToNull( proto.getArtifactId() ) );
        artifact.name( a2ACommonFieldMapper.emptyToNull( proto.getName() ) );
        artifact.description( a2ACommonFieldMapper.emptyToNull( proto.getDescription() ) );
        if ( proto.hasMetadata() ) {
            artifact.metadata( a2ACommonFieldMapper.metadataFromProto( proto.getMetadata() ) );
        }
        artifact.extensions( a2ACommonFieldMapper.emptyListToNull( proto.getExtensionsList() ) );
        artifact.parts( partListToPartArray( proto.getPartsList() ) );

        return artifact.build();
    }

    protected Part[] partListToPartArray(List<org.a2aproject.sdk.grpc.Part> list) {
        if ( list == null ) {
            return null;
        }

        Part<?>[] partTmp = new Part<?>[list.size()];
        int i = 0;
        for ( org.a2aproject.sdk.grpc.Part part : list ) {
            partTmp[i] = partMapper.fromProto( part );
            i++;
        }

        return partTmp;
    }
}
