package nova.enterprise_service_hub.mapper;

import nova.enterprise_service_hub.dto.AgencyServiceDTO;
import nova.enterprise_service_hub.dto.ImageMetadataDTO;
import nova.enterprise_service_hub.model.AgencyService;
import nova.enterprise_service_hub.model.ImageMetadata;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for AgencyService → AgencyServiceDTO.
 * Compile-time code generation — zero reflection overhead.
 */
@Mapper(componentModel = "spring")
public interface AgencyServiceMapper {

    AgencyServiceDTO toDto(AgencyService entity);

    List<AgencyServiceDTO> toDtoList(List<AgencyService> entities);

    ImageMetadataDTO toImageDto(ImageMetadata image);
}
