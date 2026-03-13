package nova.enterprise_service_hub.mapper;

import nova.enterprise_service_hub.dto.CaseStudyDTO;
import nova.enterprise_service_hub.dto.ImageMetadataDTO;
import nova.enterprise_service_hub.dto.ProjectDTO;
import nova.enterprise_service_hub.model.ImageMetadata;
import nova.enterprise_service_hub.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Project → ProjectDTO.
 * Maps structured case study fields into nested {@link CaseStudyDTO}
 * and embedded image metadata into {@link ImageMetadataDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "caseStudy", expression = "java(toCaseStudy(entity))")
    ProjectDTO toDto(Project entity);

    List<ProjectDTO> toDtoList(List<Project> entities);

    ImageMetadataDTO toImageDto(ImageMetadata image);

    List<ImageMetadataDTO> toImageDtoList(List<ImageMetadata> images);

    default CaseStudyDTO toCaseStudy(Project entity) {
        return new CaseStudyDTO(
                entity.getCaseStudyChallenge(),
                entity.getCaseStudySolution(),
                entity.getCaseStudyResult());
    }
}
