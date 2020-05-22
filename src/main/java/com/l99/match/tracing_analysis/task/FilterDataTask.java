package com.l99.match.tracing_analysis.task;

import com.alibaba.fastjson.JSONObject;
import com.l99.match.tracing_analysis.common.CommonResult;
import com.l99.match.tracing_analysis.common.DataSourceHolder;
import com.l99.match.tracing_analysis.constant.DataSourceConstant;
import com.l99.match.tracing_analysis.utils.DataSourceUtils;
import com.l99.match.tracing_analysis.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class FilterDataTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FilterDataTask.class);

    public String span;

    public FilterDataTask(String span) {
        this.span = span;
    }

    @Override
    public void run() {
        String[] split = DataSourceUtils.split(span);
        Set<String> spanSet = new HashSet<>();
        // 如果需要过滤，则将本地缓存相同traceId的span发送给汇总程序
        if (DataSourceUtils.needFilter(split[8])) {
            DataSourceHolder.addFilterTraceIdSet(split[0]);
            // 传送给后台汇总接口，如果本地有缓存删除本地缓存（当出现错误后，后面与后台汇总接口的通信次数与错误量成正比）
            Set<String> cacheSpanSet = DataSourceHolder.getHolder(split[0]);
            if (cacheSpanSet != null) {
                spanSet.addAll(cacheSpanSet);
                // remove cache
                DataSourceHolder.removeHolder(split[0]);
            }
        }

        // 查看当前 traceId 是否需要发送
        if (DataSourceHolder.containedByFilterTraceIdSet(split[0])) {
            // 发送当前span
            spanSet.add(span);
        }

        if (!spanSet.isEmpty()) {
            String result = JSONObject.toJSONString(spanSet);
            log.info("error span:\n" + result);
            sendData(result);
        } else {
            DataSourceHolder.putHolder(split[0], span);
        }
    }

    public void sendData(String spanJsonString) {
        WebUtils.postJsonData(DataSourceConstant.SUMMARY_ADDRESS, spanJsonString, CommonResult.class);
    }

}