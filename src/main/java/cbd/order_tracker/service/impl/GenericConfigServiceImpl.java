package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.dto.request.GenericConfigReqDto;
import cbd.order_tracker.model.dto.response.GenericConfigResDto;
import cbd.order_tracker.model.enumerations.ConfigType;
import cbd.order_tracker.repository.GenericConfigRepository;
import cbd.order_tracker.service.inter.GenericConfigService;
import cbd.order_tracker.util.CompanyMapper;
import cbd.order_tracker.util.GenericConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenericConfigServiceImpl implements GenericConfigService {

    private final GenericConfigRepository genericConfigRepository;

    @Override
    public List<GenericConfigResDto> getAll() {
        return genericConfigRepository.findAll()
                .stream()
                .map(GenericConfigMapper::toResDto)
                .toList();
    }

    @Override
    public List<GenericConfigResDto> getByType(ConfigType type) {
        return genericConfigRepository.findByType(type)
                .stream()
                .map(GenericConfigMapper::toResDto)
                .toList();
    }

    @Override
    public GenericConfigResDto create(GenericConfigReqDto config) {
        return GenericConfigMapper.toResDto(genericConfigRepository.save(new GenericConfig(config)));
    }

    @Override
    public GenericConfigResDto update(Long id, GenericConfigReqDto updatedConfig) {
        GenericConfig existing = genericConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found"));

        existing.setType(updatedConfig.getType());
        existing.setValue(updatedConfig.getValue());

        return GenericConfigMapper.toResDto(genericConfigRepository.save(existing));
    }

    @Override
    @Transactional
    public List<GenericConfigResDto> bulkUpdate(String type, List<GenericConfigReqDto> configs) {
        genericConfigRepository.deleteByType(type);

        List<GenericConfig> entities = configs.stream()
                .map(GenericConfig::new)
                .collect(Collectors.toList());

        List<GenericConfig> saved = genericConfigRepository.saveAll(entities);

        return saved.stream()
                .map(GenericConfigMapper::toResDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        genericConfigRepository.deleteById(id);
    }
}
