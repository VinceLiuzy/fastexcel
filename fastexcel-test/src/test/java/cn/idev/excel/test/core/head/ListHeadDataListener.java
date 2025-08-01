package cn.idev.excel.test.core.head;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.listener.ReadListener;
import com.alibaba.fastjson2.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

/**
 *
 */
@Slf4j
public class ListHeadDataListener implements ReadListener<Map<Integer, String>> {

    List<Map<Integer, String>> list = new ArrayList<Map<Integer, String>>();

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        Assertions.assertNotNull(context.readRowHolder().getRowIndex());
        headMap.forEach((key, value) -> {
            Assertions.assertEquals(value.getRowIndex(), context.readRowHolder().getRowIndex());
            Assertions.assertEquals(value.getColumnIndex(), key);
        });
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        list.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        Assertions.assertEquals(list.size(), 1);
        Map<Integer, String> data = list.get(0);
        Assertions.assertEquals("字符串0", data.get(0));
        Assertions.assertEquals("1", data.get(1));
        Assertions.assertEquals("2020-01-01 01:01:01", data.get(2));
        Assertions.assertEquals("额外数据", data.get(3));
        log.debug("First row:{}", JSON.toJSONString(list.get(0)));
    }
}
