package com.loyayz.simple;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Data
@NoArgsConstructor
public class Page<T> implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 数据集
     */
    private List<T> items;
    /**
     * 页码
     */
    private int pageNum;
    /**
     * 每页数量
     */
    private int pageSize;
    /**
     * 总记录数
     */
    private long total;
    /**
     * 总页数
     */
    private int pages;
    /**
     * 当前页偏移量（起始行）
     */
    private long offset;

    public <R> Page<R> convert(Function<? super T, ? extends R> mapper) {
        Page<R> result = new Page<>(this);
        List<R> collect = this.getItems().stream().map(mapper).collect(toList());
        result.setItems(collect);
        return result;
    }

    private Page(Page<?> other) {
        this.items = new ArrayList<>();
        this.pageNum = other.getPageNum();
        this.pageSize = other.getPageSize();
        this.total = other.getTotal();
        this.pages = other.getPages();
        this.offset = other.getOffset();
    }

}
