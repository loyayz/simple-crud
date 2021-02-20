package com.loyayz.simple.mybatis;

import com.github.pagehelper.PageHelper;
import com.loyayz.simple.Page;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public final class Pages {

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction) {
        return doSelectPage(pageNum, pageSize, listAction, null);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, int count) {
        return doSelectPage(pageNum, pageSize, listAction, () -> (long) count);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, long count) {
        return doSelectPage(pageNum, pageSize, listAction, () -> count);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, Supplier<Long> countAction) {
        boolean autoCount = countAction == null;
        com.github.pagehelper.Page<T> page = PageHelper.startPage(pageNum, pageSize, autoCount)
                .doSelectPage(listAction::get);
        if (!autoCount) {
            page.setTotal(countAction.get());
        }
        Page<T> result = new Page<>();
        result.setItems(page.getResult());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setOffset(page.getStartRow());
        return result;
    }

}
