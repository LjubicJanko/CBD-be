package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.request.GearReqDto;
import cbd.order_tracker.model.dto.response.GearResDto;
import cbd.order_tracker.service.inter.GearService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Gear entities.
 */
@RestController
@RequestMapping("/api/config/gear")
@RequiredArgsConstructor
public class GearController {

    private final GearService gearService;

    @PostMapping
    public GearResDto create(@RequestBody GearReqDto gearReqDto) {
        return gearService.create(gearReqDto);
    }

    @GetMapping
    public List<GearResDto> getAll() {
        return gearService.getAll();
    }

    @GetMapping("/category/{categoryId}")
    public List<GearResDto> getAllByCategory(@PathVariable Long categoryId) {
        return gearService.getAllOfCategory(categoryId);
    }

    @PutMapping("/edit")
    public GearResDto edit(@RequestBody GearReqDto gearReqDto) {
        return gearService.edit(gearReqDto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        gearService.delete(id);
    }
}
