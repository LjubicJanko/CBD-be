package cbd.order_tracker.controller;

import cbd.order_tracker.model.ContactInfo;
import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;
import cbd.order_tracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orderExtend")
@RequiredArgsConstructor
public class OrderExtensionController {

    private final OrderService orderService;

    @PostMapping("/create")
    public OrderExtensionDto createOrderExtension(@RequestBody OrderExtensionReqDto order) {
        return orderService.createExtension(order);
    }

    @PutMapping("editContactInfo/{id}")
    public OrderExtensionDto editContactInfo(@PathVariable Long id, @RequestBody ContactInfo contactInfo) {
        return orderService.editContactInfo(id, contactInfo);
    }

}
