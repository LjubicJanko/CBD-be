package cbd.order_tracker.controller;

import cbd.order_tracker.security.CheckCompanyAccess;
import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderPriority;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.dto.CompanyDto;
import cbd.order_tracker.model.dto.OrderDTO;
import cbd.order_tracker.model.dto.OrderOverviewDto;
import cbd.order_tracker.model.dto.PageableResponse;
import cbd.order_tracker.model.dto.response.CompanyOverviewResDto;
import cbd.order_tracker.service.CompanyService;
import cbd.order_tracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    private final OrderService orderService;


    @GetMapping("/{id}")
    @CheckCompanyAccess(paramIndex = 0)
    public CompanyDto get(@PathVariable Long id) {
        return companyService.get(id);
    }

    @PostMapping("/getCompanies")
    @CheckCompanyAccess(paramIndex = 0, list = true)
    public List<CompanyOverviewResDto> getCompanies(@RequestBody List<Long> ids) {
        System.out.println("you got access");
        return companyService.getCompanies(ids);
    }

    @GetMapping("/all")
    public List<CompanyOverviewResDto> getAll(){
        System.out.println("you got access");
        return companyService.getAll();
    }

    @PostMapping("/create")
    public CompanyDto create(@RequestBody CompanyDto companyDto) {
        return companyService.create(companyDto);
    }

    @PostMapping("/addOrder/{companyId}")
    public OrderDTO addOrder(@PathVariable Long companyId, @RequestBody OrderRecord order) {
        return orderService.createOrder(companyId, order);
    }

    @GetMapping("/{companyId}/orders")
    public ResponseEntity<PageableResponse<OrderOverviewDto>> getOrders(
            @PathVariable Long companyId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(required = false) List<OrderPriority> priorities,
            @RequestParam(required = false) String sortCriteria,
            @RequestParam(required = false) String sort,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer perPage,
            @RequestParam(required = false) List<OrderExecutionStatus> executionStatuses) {

            // Apply default filter for executionStatuses if not provided
            if (executionStatuses == null || executionStatuses.isEmpty()) {
                executionStatuses = List.of(OrderExecutionStatus.ACTIVE, OrderExecutionStatus.PAUSED);
            }

            // Delegate to the service layer with all parameters
            PageableResponse<OrderOverviewDto> response = orderService
                    .fetchPageable(companyId, searchTerm, statuses, priorities, sortCriteria, sort, executionStatuses, page, perPage);

            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{companyId}/updateEmployees")
    public CompanyDto updateEmployees(
            @PathVariable Long companyId,
            @RequestBody List<Integer> employeeIds)
    {
        return companyService.updateEmployees(companyId, employeeIds);
    }
}
