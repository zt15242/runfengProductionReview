package other.rainbow.service.utils;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.ProductCategory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.XObject;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;;

public class DataMaintenanceService {
    
//    private static final Logger logger = LoggerFactory.getLogger(DataMaintenanceService.class);
    
    /**
     * 异步SQL查询销售易CRM数据
     * @param sql SQL查询语句
     * @param includePrivate 是否包含私有数据
     * @param includeDeleted 是否包含已删除数据
     * @return CompletableFuture<QueryResult<JSONObject>> 异步返回查询结果
     */
    public CompletableFuture<QueryResult<JSONObject>> asyncQueryCrmDataBySql(
            String sql, 
            boolean includePrivate, 
            boolean includeDeleted) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
//                logger.info("开始异步SQL查询CRM数据, sql: {}", sql);
                
                QueryResult<JSONObject> result = XoqlService.instance()
                    .query(sql, includePrivate, includeDeleted);
                
//                logger.info("异步SQL查询CRM数据完成, 获取到 {} 条记录", result.getRecords().size());
                return result;
                
            } catch (Exception e) {
//                logger.error("异步SQL查询CRM数据失败", e);
                throw new RuntimeException("SQL查询CRM数据失败: " + e.getMessage());
            }
        });
    }

    /**
     * 异步批量插入销售易CRM数据
     * @param dataList 要插入的数据列表
     * @param checkRepeat 是否检查重复
     * @param ignoreError 是否忽略错误
     * @return CompletableFuture<BatchOperateResult> 异步返回批量插入结果
     */
    public <T extends XObject> CompletableFuture<BatchOperateResult> asyncBatchInsertCrmData(
            List<T> dataList,
            boolean checkRepeat,
            boolean ignoreError) {
        return CompletableFuture.supplyAsync(() -> {
            try {
//                logger.info("开始异步批量插入CRM数据, 数据条数: {}", dataList.size());
                
                BatchOperateResult result = XObjectService.instance()
                    .insert(dataList, checkRepeat, ignoreError);

                return result;
                
            } catch (Exception e) {
//                logger.error("异步批量插入CRM数据失败", e);
                throw new RuntimeException("批量插入CRM数据失败: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        DataMaintenanceService service = new DataMaintenanceService();
        
        // 测试查询
//        testQuery(service);
        
        // 测试批量插入
        testBatchInsert(service);
        
        // 等待异步操作完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void testQuery(DataMaintenanceService service) {
        String sql = "select id, name from goods";
        
        CompletableFuture<QueryResult<JSONObject>> future = 
            service.asyncQueryCrmDataBySql(sql, true, true);
            
        future.thenAccept(result -> {
            List<JSONObject> records = result.getRecords();
            System.out.println("查询到记录数: " + records.size());
            records.forEach(record -> {
                System.out.println("记录ID: " + record.getString("id"));
                System.out.println("记录名称: " + record.getString("name"));
                System.out.println("------------------------");
            });
        }).exceptionally(ex -> {
            System.err.println("查询失败: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }
    
    private static void testBatchInsert(DataMaintenanceService service) {
        // 创建测试数据列表
        List<ProductCategory> goodsList = new ArrayList<>();
        ProductCategory pro = new ProductCategory();
        pro.setName("测试");

        ProductCategory pro2 = new ProductCategory();
        pro2.setName("测试2");
        goodsList.add(pro);
        goodsList.add(pro2);
        // 调用批量插入方法
        CompletableFuture<BatchOperateResult> future =
            service.asyncBatchInsertCrmData(goodsList, true, true);
        future.thenAccept(result -> {
            System.out.println("批量插入完成");
            // 如果有失败的记录，打印失败信息
            System.out.println(JSON.toJSONString(result.getOperateResults()));
        }).exceptionally(ex -> {
            System.err.println("批量插入失败: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }
}
