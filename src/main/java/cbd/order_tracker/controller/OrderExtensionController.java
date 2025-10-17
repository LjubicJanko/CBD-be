package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.request.OrderExtensionReqDto;
import cbd.order_tracker.model.dto.response.OrderExtensionDto;
import cbd.order_tracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orderExtend")
@RequiredArgsConstructor
public class OrderExtensionController {

    private final OrderService orderService;

    @PostMapping("/create")
    public OrderExtensionDto createOrderExtension(@RequestBody OrderExtensionReqDto order) {
        return  orderService.createExtension(order);
    }

}
