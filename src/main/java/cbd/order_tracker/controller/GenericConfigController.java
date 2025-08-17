package cbd.order_tracker.controller;
import cbd.order_tracker.model.dto.request.GenericConfigReqDto;
import cbd.order_tracker.model.dto.response.GenericConfigResDto;
import cbd.order_tracker.model.enumerations.ConfigType;
import cbd.order_tracker.security.super_admin.CheckSuperAdmin;
import cbd.order_tracker.service.inter.GenericConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class GenericConfigController {

    private final GenericConfigService genericConfigService;

    // List all configs
    @GetMapping
    public List<GenericConfigResDto> getAll() {
        return genericConfigService.getAll();
    }

    // List by type
    @GetMapping("/type/{type}")
    public List<GenericConfigResDto> getByType(@PathVariable ConfigType type) {
        return genericConfigService.getByType(type);
    }

    // Create new config (super admin only)
    @PostMapping("/create")
    @CheckSuperAdmin
    public GenericConfigResDto create(@RequestBody GenericConfigReqDto config) {
        return genericConfigService.create(config);
    }

    // Update config (super admin only)
    @PutMapping("/{id}")
    @CheckSuperAdmin
    public GenericConfigResDto update(@PathVariable Long id, @RequestBody GenericConfigReqDto config) {
        return genericConfigService.update(id, config);
    }

    /**
     * Bulk update/replace all configs of a given type.
     * Existing configs of that type will be removed and replaced with the provided list.
     */
    @PutMapping("/bulk/{type}")
    @CheckSuperAdmin
    public List<GenericConfigResDto> bulkUpdate(
            @PathVariable String type,
            @RequestBody List<GenericConfigReqDto> configs
    ) {
        return genericConfigService.bulkUpdate(type, configs);
    }

    // Delete config (super admin only)
    @DeleteMapping("/{id}")
    @CheckSuperAdmin
    public void delete(@PathVariable Long id) {
        genericConfigService.delete(id);
    }
}
