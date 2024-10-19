package cbd.order_tracker.model.dto;

import java.util.List;

public class PageableResponse<T> {
    public Integer page;
    public Integer perPage;
    public Integer total;
    public List<T> data;

    public PageableResponse(){}

    public PageableResponse(Integer page, Integer perPage, Integer total, List<T> data) {
        this.page = page;
        this.perPage = perPage;
        this.total = total;
        this.data = data;
    }
}
