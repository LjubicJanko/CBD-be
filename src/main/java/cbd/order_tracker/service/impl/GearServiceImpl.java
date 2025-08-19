package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.config.Gear;
import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.dto.request.GearReqDto;
import cbd.order_tracker.model.dto.response.GearResDto;
import cbd.order_tracker.model.enumerations.ConfigType;
import cbd.order_tracker.repository.GearRepository;
import cbd.order_tracker.repository.GenericConfigRepository;
import cbd.order_tracker.service.inter.GearService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the GearService interface.
 * Handles creation and retrieval of Gear entities.
 */
@Service
@RequiredArgsConstructor
public class GearServiceImpl implements GearService {

    private final GearRepository gearRepository;
    private final GenericConfigRepository configRepository;

    @Override
    public GearResDto create(GearReqDto gearReqDto) {
        GenericConfig category = configRepository.findById(gearReqDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (category.getType() != ConfigType.GEAR_CATEGORY) {
            throw new IllegalArgumentException("Invalid config type. Expected GEAR_CATEGORY.");
        }

        Gear gear = new Gear();
        gear.setName(gearReqDto.getName());
        gear.setCategory(category);

        Gear saved = gearRepository.save(gear);
        return new GearResDto(saved);
    }

    @Override
    public List<GearResDto> getAll() {
        return gearRepository.findAll().stream()
                .map(GearResDto::new)
                .toList();
    }

    @Override
    public List<GearResDto> getAllOfCategory(Long categoryId) {
        return gearRepository.findAll().stream()
                .filter(g -> g.getCategory() != null && g.getCategory().getId().equals(categoryId))
                .map(GearResDto::new)
                .toList();
    }

    @Override
    public GearResDto edit(GearReqDto gearReqDto) {
        Gear gear = gearRepository.findById(gearReqDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Gear not found with id: " + gearReqDto.getId()));

        gear.setName(gearReqDto.getName());

        var category = configRepository.findById(gearReqDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + gearReqDto.getCategoryId()));
        gear.setCategory(category);

        gear = gearRepository.save(gear);
        return new GearResDto(gear);
    }

    @Override
    public void delete(Long id) {
        if (!gearRepository.existsById(id)) {
            throw new EntityNotFoundException("Gear with id " + id + " not found");
        }
        gearRepository.deleteById(id);
    }
}
