package com.meiya.skillsmap.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ListResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public ListResult() {}

    public ListResult(List<T> records, long total, long page, long size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> ListResult<T> of(List<T> records, long total, long page, long size) {
        return new ListResult<>(records, total, page, size);
    }

    public static <T> ListResult<T> empty(long page, long size) {
        return new ListResult<>(Collections.emptyList(), 0, page, size);
    }

    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
