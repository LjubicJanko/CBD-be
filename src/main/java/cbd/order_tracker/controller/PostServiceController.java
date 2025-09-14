package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.request.PostServiceReqDto;
import cbd.order_tracker.service.inter.PostServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Gear entities.
 */
@RestController
@RequestMapping("/api/postService")
@RequiredArgsConstructor
public class PostServiceController {

    private final PostServiceService postServiceService;

    @PostMapping
    public PostServiceReqDto create(@RequestBody PostServiceReqDto postServiceReqDto) {
        return postServiceService.create(postServiceReqDto);
    }

    @GetMapping
    public List<PostServiceReqDto> getAll() {
        return postServiceService.getAll();
    }

    @PutMapping("/edit")
    public PostServiceReqDto edit(@RequestBody PostServiceReqDto postServiceReqDto) {
        return postServiceService.edit(postServiceReqDto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        postServiceService.delete(id);
    }
}
