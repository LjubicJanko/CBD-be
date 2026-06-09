package cbd.order_tracker.controller;

import cbd.order_tracker.config.FeatureGuard;
import cbd.order_tracker.config.TenantContext;
import cbd.order_tracker.model.ContactInfo;
import cbd.order_tracker.model.Tenant;
import cbd.order_tracker.model.enums.Feature;
import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;
import cbd.order_tracker.repository.TenantRepository;
import cbd.order_tracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orderExtend/{tenantSlug}")
@RequiredArgsConstructor
public class OrderExtensionController {

    private final OrderService orderService;
    private final TenantRepository tenantRepository;
    private final FeatureGuard featureGuard;

    private Tenant resolveTenant(String tenantSlug) {
        Tenant tenant = tenantRepository.findBySlug(tenantSlug.toLowerCase())
                .filter(Tenant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        TenantContext.setTenantId(tenant.getId());
        return tenant;
    }

    @PostMapping("/create")
    public OrderExtensionDto createOrderExtension(@PathVariable String tenantSlug, @RequestBody OrderExtensionReqDto order) {
        Tenant tenant = resolveTenant(tenantSlug);
        try {
            featureGuard.requireFeature(tenant, Feature.ORDER_EXTENSION.getKey());
            return orderService.createExtension(order, tenant);
        } finally {
            TenantContext.clear();
        }
    }

    @PutMapping("/editContactInfo/{trackingId}")
    public OrderExtensionDto editContactInfo(@PathVariable String tenantSlug, @PathVariable String trackingId, @RequestBody ContactInfo contactInfo) {
        Tenant tenant = resolveTenant(tenantSlug);
        try {
            featureGuard.requireFeature(tenant, Feature.ORDER_EXTENSION.getKey());
            return orderService.editContactInfo(trackingId, contactInfo);
        } finally {
            TenantContext.clear();
        }
    }

    @PutMapping("/edit/{trackingId}")
    public OrderExtensionDto editExtension(@PathVariable String tenantSlug, @PathVariable String trackingId, @RequestBody OrderExtensionReqDto dto) {
        Tenant tenant = resolveTenant(tenantSlug);
        try {
            featureGuard.requireFeature(tenant, Feature.ORDER_EXTENSION.getKey());
            return orderService.editExtension(trackingId, dto);
        } finally {
            TenantContext.clear();
        }
    }
}
