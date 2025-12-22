package cbd.order_tracker.controller;

import cbd.order_tracker.model.dto.request.BannerReqDto;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import cbd.order_tracker.model.enums.PageType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannersController {

    private final BannerService bannerService;

    @PostMapping("/create")
    public ResponseEntity<BannerResDto> createBanner(@RequestBody BannerReqDto bannerReqDto) {
        return new ResponseEntity<>(bannerService.createBanner(bannerReqDto), HttpStatus.OK);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<BannerResDto> editBanner(@PathVariable Long id, @RequestBody BannerReqDto bannerReqDto) {
        return new ResponseEntity<>(bannerService.editBanner(id, bannerReqDto), HttpStatus.OK);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<BannerResDto>> getAll() {
        return new ResponseEntity<>(bannerService.getAll(), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
    }

    @GetMapping("/published")
    public ResponseEntity<BannerResDto> getPublished() {
        return ResponseEntity.ok(bannerService.getPublished());
    }

    @GetMapping("/active")
    public ResponseEntity<BannerResDto> getActiveBanner() {
        return ResponseEntity.ok(bannerService.getActiveBanner());
    }


    @PostMapping("/publish/{id}")
    public ResponseEntity<BannerResDto> publish(
            @PathVariable Long id,
            @RequestBody List<PageType> pages
    ) {
        return ResponseEntity.ok(bannerService.publish(id, pages));
    }

    @PostMapping("/unpublish/{id}")
    public void unpublish(
            @PathVariable Long id
    ) {
        bannerService.unpublish(id);
    }

}
