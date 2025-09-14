package cbd.order_tracker.service.impl;

import cbd.order_tracker.model.dto.request.PostServiceReqDto;
import cbd.order_tracker.model.post_service.PostService;
import cbd.order_tracker.repository.PostServiceRepository;
import cbd.order_tracker.service.inter.PostServiceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceServiceImpl implements PostServiceService {

    private final PostServiceRepository postServiceRepository;

    @Override
    public PostServiceReqDto create(PostServiceReqDto postServiceReqDto) {
        PostService postService = new PostService(postServiceReqDto);
        PostService saved = postServiceRepository.save(postService);

        return new PostServiceReqDto(saved);
    }

    @Override
    public List<PostServiceReqDto> getAll() {
        return postServiceRepository.findAll().stream()
                .map(PostServiceReqDto::new)
                .toList();
    }

    @Override
    public PostServiceReqDto edit(PostServiceReqDto postServiceReqDto) {
        PostService postService = postServiceRepository.findById(postServiceReqDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Post service not found with id: " + postServiceReqDto.getId()));

        postService.setName(postServiceReqDto.getName());
        postService.setWebsiteUrl(postServiceReqDto.getWebsiteUrl());
        PostService saved = postServiceRepository.save(postService);

        return new PostServiceReqDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!postServiceRepository.existsById(id)) {
            throw new EntityNotFoundException("Post service with id " + id + " not found");
        }
        postServiceRepository.deleteById(id);
    }
}
