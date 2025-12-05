package other.rainbow.orderapp.batchjob;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.data.model.Factory__c;
import com.rkhd.platform.sdk.exception.*;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.BatchJobData;
import com.rkhd.platform.sdk.task.param.BatchJobDataBuilder;
import com.rkhd.platform.sdk.task.param.BatchJobParam;
import com.rkhd.platform.sdk.task.param.PrepareParam;
import other.rainbow.orderapp.api.ProductSalesInfoSyncToSAP;
import other.rainbow.orderapp.approval.orderApprovalBefore;
import other.rainbow.orderapp.pojo.items;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/12/4 17:42
 * @e-mail 2717718875@qq.com
 * @description:产品库存数据同步BatchJob
 */
public class ProductSalesInfoSyncBatchJob implements BatchJobPro<XObject> {
    private static final Logger log = LoggerFactory.getLogger();
    private final static XObjectService xs = XObjectService.instance();
    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        String sql = "select id,name from factory__c";
        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();
//        prepareParam.getParamMap().forEach(batchJobDataBuilder::setBatchJobParam);
        return batchJobDataBuilder
                .setSql(sql)    // 设置sql
                .setAdmin(true) // 设置管理员权限
                .setBatchJobParam("param1", "value1") // 可以单独在这里设置BatchJobParam
                .buildQueryData();
    }

    @Override
    public void execute(List<XObject> list, BatchJobParam batchJobParam) {
        // list为在param中sql的查询结果job切割后的结果
        List<items> itemsList = new ArrayList<>();
        for (XObject xObject : list) {
            Factory__c factory = (Factory__c)xObject;
            log.info("factory:"+factory);
            items items = new items();
            items.setWerks(factory.getName());
            // items.setMatnr("*");
            items.setMatnr("");
            itemsList.add(items);
        }
        log.info(JSON.toJSONString(itemsList));
        try {
            ProductSalesInfoSyncToSAP.crmToSap(itemsList);
        } catch (XsyHttpException e) {
            throw new RuntimeException(e);
        } catch (ScriptBusinessException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        } catch (CustomConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {

    }

    public static void main(String[] args) throws Exception {
       /* QueryResult<XObject> query = XObjectService.instance().query("select id,name from factory__c");
        System.out.println(JSON.toJSONString( query.getRecords()));*/
        Long num = Long.valueOf("3607258151715877");
        ApprovalEventRequest aer = new ApprovalEventRequest();
        aer.setDataId(num);
//        orderApprovalBefore.execute(aer);
        ProductSalesInfoSyncToSAP psts = new ProductSalesInfoSyncToSAP();
        psts.productSalesInfo("3631502800471081","");

    }
}
