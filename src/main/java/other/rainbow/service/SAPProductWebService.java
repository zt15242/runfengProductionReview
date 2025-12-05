package other.rainbow.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.*;
import com.rkhd.platform.sdk.data.model.*;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.FutureTaskService;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.rainbow.service.futuretask.FutureTaskDemoImpl;
import other.rainbow.service.pojo.*;
import other.rainbow.service.utils.DataMaintenanceService;
import other.rainbow.service.utils.InterfaceLogReq;
import other.rainbow.service.utils.ObjectOptionValueRetrieval;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 武于伦
 * @version 1.0
 * @date 2024/11/20 9:23
 * @e-mail 2717718875@qq.com
 * @description: 商品同步
 */
@RestApi(baseUrl = "/service/apexrest")
public class SAPProductWebService {
    Logger logger = LoggerFactory.getLogger();
    HashMap<String, Long> unitMaps = new HashMap<>();//存储单位
    private  Map<String, Map<String, Integer>> allOptions;
    private  Map<String, Map<String, Integer>> allOptionsP;
    private List<Goods> insertgoods = new ArrayList<>();
    private Boolean isSuccess = true;
    private String msg;

    private Long entityTypeID;
    private Long productEntityTypeID;
    private Long goodsUnitEntityTypeID;
    private Long UnitentityTypeID;
    private DataMaintenanceService service = new DataMaintenanceService();
    /**
     * SAP商品同步Web服务类
     * 用于处理SAP系统商品数据同步到当前系统的相关操作
     */
    @RestMapping(value = "/SAPProductWebService",method = RequestMethod.POST)
    public String doPost(@RestBeanParam(name = "jsonObject") JSONObject jsonObject) throws ApiEntityServiceException {
        ResponseBody response = new ResponseBody();

        // 初始化 ResultInfo
        RespCustomerInfo resultInfo = new RespCustomerInfo();
        resultInfo.setMsgty("S");
        resultInfo.setMsgtx("处理成功");
        response.setResultinfo(resultInfo);

        // 初始化 EsbInfo
        RespESInfo esbInfo = new RespESInfo();
        esbInfo.setReturnstatus("S");
        esbInfo.setReturncode("S001");
        esbInfo.setResponsetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        esbInfo.setRequesttime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        response.setEsbinfo(esbInfo);
        try {
            if (jsonObject == null) {
                setErrorResponse(response, "请求参数为空");
                return JSON.toJSONString(response);
            }
            RequestParameters requestParameters = jsonObject.toJavaObject(RequestParameters.class);
            if (requestParameters == null || requestParameters.resultinfo == null || requestParameters.resultinfo.isEmpty()) {
                setErrorResponse(response, "请求参数格式错误或数据为空");
                return JSON.toJSONString(response);
            }
            // TODO 大量数据采用异步处理 分别记录日志 目前只要传入商品超出20 就采用异步
            if (requestParameters.resultinfo.size()>20){
                // 分批处理数据
                List<ReqProductInfo> allData = requestParameters.resultinfo;
                int batchSize = 20;

                for (int i = 0; i < allData.size(); i += batchSize) {
                    // 获取当前批次的数据
                    int endIndex = Math.min(i + batchSize, allData.size());
                    List<ReqProductInfo> batchData = allData.subList(i, endIndex);

                    // 将当前批次转换为JSON字符串
                    String batchJson = JSON.toJSONString(batchData);
                    logger.info("批次 " + (i/batchSize + 1) + " 数据: " + batchJson);
                    FutureTaskService.instance().addFutureTask(FutureTaskDemoImpl.class,batchJson);
                    // 可选：添加延时避免请求过于频繁
                    if (i + batchSize < allData.size()) {
                        Thread.sleep(100); // 100ms延时
                    }
                }
                return JSON.toJSONString(response);
            }
            // 获取选项字段  2次
            allOptions = ObjectOptionValueRetrieval.getAllFieldOptions("goods");
            allOptionsP = ObjectOptionValueRetrieval.getAllFieldOptions("product");
            // 提取单位和商品名称
            Set<String> units = extractUnits(requestParameters.resultinfo);
            List<String> goodsNames = extractGoodsNames(requestParameters.resultinfo);

            logger.info("units: " + JSON.toJSONString(units));
            logger.info("传入进来的商品：" + JSON.toJSONString(goodsNames));

            try {
                // 查询现有单位   商品
                Map<String, Long> existingUnits = queryExistingUnits(units);
                Map<String, Goods> existingGoods = queryExistingGoods(goodsNames);
                // 记录类型 4次   共6次
                entityTypeID = MetadataService.instance().getBusiType("goods", "defaultBusiType").getId();
                productEntityTypeID = MetadataService.instance().getBusiType("product", "defaultBusiType").getId();
                goodsUnitEntityTypeID = MetadataService.instance().getBusiType("goodsUnit", "defaultBusiType").getId();
                UnitentityTypeID = MetadataService.instance().getBusiType("Unit", "defaultBusiType").getId();
                // 处理数据
                ProcessResult result = processProductData(requestParameters.resultinfo, existingUnits, existingGoods);

                // 执行数据操作
                handleDataOperations(result, requestParameters.resultinfo, response);
            } catch (Exception e) {
                String errorMsg = "处理数据时发生错误: " + e;
                logger.error(errorMsg, e);
                msg = errorMsg;
                isSuccess = false;
                setErrorResponse(response, errorMsg);
            }

        } catch (Exception e) {
            String errorMsg = "处理请求失败: " + e.getMessage();
            logger.error(errorMsg, e);
            msg = errorMsg;
            isSuccess = false;
            setErrorResponse(response, errorMsg);
        }
        List<String> dateIdList = new ArrayList<>();
        // 1次  7次
        InterfaceLogReq.insertInterfaceLog("SAPProductWebService", "SAP",
                "CRM", "/rest/data/v2.0/scripts/api/service/apexrest/SAPProductWebService", JSON.toJSONString(jsonObject), isSuccess, false,
                JSON.toJSONString(response), msg, "POST",dateIdList,
                "产品主数据同步", "产品主数据同步");
        return JSON.toJSONString(response);
    }
    // 批量数据处理调用
    public void batchProcessing(List<ReqProductInfo> requestParameters){
        // 获取选项字段  2次
        allOptions = ObjectOptionValueRetrieval.getAllFieldOptions("goods");
        allOptionsP = ObjectOptionValueRetrieval.getAllFieldOptions("product");
        // 提取单位和商品名称
        Set<String> units = extractUnits(requestParameters);
        List<String> goodsNames = extractGoodsNames(requestParameters);

        logger.info("units: " + JSON.toJSONString(units));
        logger.info("传入进来的商品：" + JSON.toJSONString(goodsNames));

        try {
            // 查询现有单位   商品
            Map<String, Long> existingUnits = queryExistingUnits(units);
            Map<String, Goods> existingGoods = queryExistingGoods(goodsNames);
            // 记录类型 4次   共6次
            entityTypeID = MetadataService.instance().getBusiType("goods", "defaultBusiType").getId();
            productEntityTypeID = MetadataService.instance().getBusiType("product", "defaultBusiType").getId();
            goodsUnitEntityTypeID = MetadataService.instance().getBusiType("goodsUnit", "defaultBusiType").getId();
            UnitentityTypeID = MetadataService.instance().getBusiType("Unit", "defaultBusiType").getId();
            // 处理数据
            ProcessResult result = processProductData(requestParameters, existingUnits, existingGoods);

            // 执行数据操作
            handleDataOperations(result, requestParameters, null);
        } catch (Exception e) {
            String errorMsg = "处理数据时发生错误: " + e;
            logger.error(errorMsg, e);
            throw new CustomException(errorMsg);
        }
    }
    // 初始化响应对象
    private void initializeResponse(ResponseBody response) {
        RespCustomerInfo resultInfo = new RespCustomerInfo();
        resultInfo.setMsgty("S");
        resultInfo.setMsgtx("处理成功");
        response.setResultinfo(resultInfo);

        RespESInfo esbInfo = new RespESInfo();
        esbInfo.setReturnstatus("S");
        esbInfo.setReturncode("S001");
        esbInfo.setResponsetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        esbInfo.setRequesttime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        response.setEsbinfo(esbInfo);
    }

    /**
     * 设置错误响应
     */
    private void setErrorResponse(ResponseBody response, String errorMsg) {
        if (response.getResultinfo() == null) {
            response.setResultinfo(new RespCustomerInfo());
        }
        if (response.getEsbinfo() == null) {
            response.setEsbinfo(new RespESInfo());
        }
        msg = errorMsg;
        isSuccess = false;
        response.getResultinfo().setMsgty("E");
        response.getResultinfo().setMsgtx("处理失败");
        response.getEsbinfo().setReturnstatus("E");
        response.getEsbinfo().setReturncode("E001");
        response.getEsbinfo().setReturnmsg(errorMsg);
        response.getEsbinfo().setResponsetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        response.getEsbinfo().setRequesttime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    /**
     * 添加错误信息到响应中
     */
    private void addError(ResponseBody response, String error) {
        if (response.getEsbinfo().getReturnmsg() == null) {
            response.getEsbinfo().setReturnmsg(error);
        } else {
            response.getEsbinfo().setReturnmsg(
                    response.getEsbinfo().getReturnmsg() + "; " + error);
        }
        msg = error;
        isSuccess = false;
        response.getResultinfo().setMsgty("E");
        response.getResultinfo().setMsgtx("处理失败");
        response.getEsbinfo().setReturnstatus("E");
        response.getEsbinfo().setReturncode("E001");
    }

    /**
     * 从请求参数中提取所有单位信息
     * @param params 请求参数对象
     * @return 单位名称列表
     */
    public Set<String> extractUnits(List<ReqProductInfo> params) {
        return  params.stream()
                .flatMap(resultInfo -> resultInfo.units.stream()
                        .map(unit -> unit.meinh))
                .collect(Collectors.toSet());
    }

    /**
     * 从请求参数中提取所有商品名称
     * @param params 请求参数对象
     * @return 商品名称列表
     */
    public List<String> extractGoodsNames(List<ReqProductInfo> params) {
        return params.stream()
                .flatMap(r -> r.salev.stream()
                        .map(salesInfoRow -> r.matnr + "[" + salesInfoRow.vkorg + "]"))
                .collect(Collectors.toList());
    }

    /**
     * 查询系统中已存在的单位信息
     */
    public Map<String, Long> queryExistingUnits(Set<String> units) throws ApiEntityServiceException {
//        String sql = "Select id,name from unit where UPPER(name) in (" +
//                parseListToStr2(units.stream().map(String::toUpperCase).collect(Collectors.toList())) + ")";
        QueryResult<JSONObject> result = XoqlService.instance()
                .query("Select id,name from unit where UPPER(name) in (" +
                        parseListToStr2(units.stream().map(String::toUpperCase).collect(Collectors.toList())) + ")",true,true);
        Map<String, Long> unitMap = new HashMap<>();
        for (JSONObject record : result.getRecords()) {
            unitMap.put(record.getString("name"),record.getLong("id"));
        }
        return unitMap;
    }

    /**
     * 查询系统中已存在的商品信息
     * @param goodsNames 商品名称列表
     * @return 商品名称到商品对象的映射
     */
    public Map<String, Goods> queryExistingGoods(List<String> goodsNames) throws ApiEntityServiceException, IOException {
        logger.info("本批次商品："+goodsNames);
        String sql = "select id,name,category,External_matnr__c from goods where External_matnr__c in (" + parseListToStr2(goodsNames) + ")";
        logger.info("商品查询sql："+sql);
        QueryResult<JSONObject> result = XoqlService.instance()
                .query(sql,true,true);
        logger.info("商品数据："+result.getRecords());
        Map<String, Goods> goodsMap = new HashMap<>();
        for (JSONObject record : result.getRecords()) {
			// 20250606 ljh 用External_matnr__c作为唯一商品标识 start
            // goodsMap.put(record.getString("name"), JSON.toJavaObject(record,Goods.class));
			goodsMap.put(record.getString("External_matnr__c"), JSON.toJavaObject(record,Goods.class));
			// 20250606 ljh 用External_matnr__c作为唯一商品标识 end
        }
        return goodsMap;
//        logger.info("本批次商品：" + goodsNames);
//        Map<String, Goods> goodsMap = new HashMap<>();
//
//        // 分批处理，每批20条
//        final int BATCH_SIZE = 20;
//        for (int i = 0; i < goodsNames.size(); i += BATCH_SIZE) {
//            // 获取当前批次的数据
//            int endIndex = Math.min(i + BATCH_SIZE, goodsNames.size());
//            List<String> batchNames = goodsNames.subList(i, endIndex);
//
//            // 构建查询SQL
//            String sql = "select id,name,category,External_matnr__c from goods where External_matnr__c in ("
//                + parseListToStr2(batchNames) + ")";
//            logger.info("批次 " + (i/BATCH_SIZE + 1) + " 商品查询sql：" + sql);
//
//            try {
//                // 执行查询
//                RkhdHttpClient instance = RkhdHttpClient.instance();
//                JSONArray jsonArray = NeoCrmRkhdService.v2QueryBySql(instance, sql);
//                logger.info("批次 " + (i/BATCH_SIZE + 1) + " 查询结果：" + JSON.toJSONString(jsonArray));
//
//                // 处理查询结果
//                List<Goods> goods = jsonArray.toJavaList(Goods.class);
//                for (Goods object : goods) {
//                    goodsMap.put(object.getName(), object);
//                }
//
//                // 添加延时，避免请求过于频繁
//                if (i + BATCH_SIZE < goodsNames.size()) {
//                    Thread.sleep(100); // 100ms延时
//                }
//
//            } catch (Exception e) {
//                logger.error("批次 " + (i/BATCH_SIZE + 1) + " 查询失败：" + e.getMessage(), e);
//                throw new ApiEntityServiceException("查询商品失败", e);
//            }
//        }
//
//        logger.info("查询完成，共获取到 " + goodsMap.size() + " 个商品");
//        return goodsMap;
    }

    /**
     * 将列表转换为SQL IN子句格式的字符串
     * @param list 需要转换的列表
     * @return 格式化后的字符串，如: 'item1', 'item2'
     */
    public static String parseListToStr2(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(item -> "'" + item + "'")
                .collect(Collectors.joining(", "));
    }

    /**
     * 数据处理结果类，用于存储需要新增和更新的数据
     */
    private static class ProcessResult {
        /** 需要新增的单位列表 */
        List<String> newUnits = new ArrayList<>();
        /** 需要新增的商品信息列表 */
        List<ReqProductInfo> newProducts = new ArrayList<>();
        /** 需要更新的商品映射 */
        Map<String, Goods> updateProducts = new HashMap<>();
    }

    /**
     * 处理商品数据，识别需要新增和更新的内容
     * @param params 请求参数
     * @param existingUnits 已存在的单位
     * @param existingGoods 已存在的商品
     * @return 处理结果对象
     */
    public ProcessResult processProductData(List<ReqProductInfo> params,
                                            Map<String, Long> existingUnits,
                                            Map<String, Goods> existingGoods) {
        ProcessResult result = new ProcessResult();
        Set<String> processedProducts = new HashSet<>();

        for (ReqProductInfo productInfo : params) {
            // 处理单位
            productInfo.units.stream()
                    .map(unit -> unit.meinh)
                    .filter(unitName -> !existingUnits.containsKey(unitName))
                    .forEach(result.newUnits::add);

            // 处理商品
            for (SalesInfoRow salesInfo : productInfo.salev) {
                String key = null;
                if(productInfo.ltext_e != null){
                    key = productInfo.ltext_e + "[" + salesInfo.vkorg + "]";
                }else if (productInfo.ltext_1 != null){
                    key = productInfo.ltext_1 + "[" + salesInfo.vkorg + "]";
                }else {
                    key = productInfo.matnr + "[" + salesInfo.vkorg + "]";
                }
				// 20250606 ljh 用External_matnr__c作为唯一商品标识 start key→keyNew
				String keyNew = productInfo.matnr + "[" + salesInfo.vkorg + "]";
                if (!existingGoods.containsKey(keyNew)) {
                    if (!processedProducts.contains(keyNew)) {
                        result.newProducts.add(productInfo);
                        processedProducts.add(keyNew);
                    }
                } else {
                    result.updateProducts.put(keyNew, existingGoods.get(keyNew));
                }
				// 20250606 ljh 用External_matnr__c作为唯一商品标识 start
            }
        }

        return result;
    }

    /**
     * 执行数据操作，包括创建新单位、更新和新增商品
     * 按照优先级顺序执行：1.创建单位 2.更新商品 3.新增商品
     * 如果前序操作失败，将终止后续操作
     *
     * @param result 数据处理结果，包含需要处理的数据
     * @param requestParameters 原始请求参数
     * @param response 响应对象，用于记录处理结果和错误信息
     * @throws ApiEntityServiceException 当API调用出错时抛出
     */
    private void handleDataOperations(ProcessResult result, List<ReqProductInfo> requestParameters,
                                      ResponseBody response) throws ApiEntityServiceException {
        // 首先创建新单位
        if (!result.newUnits.isEmpty()) {
            try {
                GeneratingUnits(result.newUnits, response);
                Thread.sleep(100); // 添加间隔
            } catch (Exception e) {
                addError(response, "创建单位失败: " + e.getMessage());
                return; // 如果单位创建失败，终止后续操作
            }
        }

        // 优先处理更新操作
        if (!result.updateProducts.isEmpty()) {
            logger.info("开始更新 " + result.updateProducts.size() + " 个商品");
            try {
                UPGoods(result.updateProducts, requestParameters, response);
                Thread.sleep(100); // 添加间隔
            } catch (Exception e) {
                addError(response, "更新商品失败: " + e.getMessage());
                return; // 如果更新失败，终止后续操作
            }
        }

        // 最后处理新增操作
        if (!result.newProducts.isEmpty()) {
            logger.info("开始新增 " + result.newProducts.size() + " 个商品");
            try {
                addGoods(result.newProducts, response);
            } catch (Exception e) {
                logger.info("创建商品失败数据集合："+ JSON.toJSONString(result.newProducts));
                addError(response, "创建商品失败: " + e.getMessage());
            }
        }
    }

    /**
     * 生成单位并返回名称与ID的映射
     * 1. 检查现有单位，避免重复创建
     * 2. 创建新单位
     * 3. 更新单位映射关系
     *
     * @param units 需要创建的单位名称列表
     * @param response 响应对象，用于记录处理结果和错误信息
     * @throws ApiEntityServiceException 当API调用出错时抛出
     */
    public void GeneratingUnits(List<String> units, ResponseBody response) throws ApiEntityServiceException {
        if (units == null || units.isEmpty()) {
            logger.warn("没有需要创建的单位");
            return;
        }

        // 先查询现有单位
        Map<String, Unit> existingUnits = new HashMap<>();
        // 1次  8次
        QueryResult<JSONObject> existingQuery = XoqlService.instance()
                .query("Select id,name from unit");
        for (JSONObject unit : existingQuery.getRecords()) {
            existingUnits.put(unit.getString("name"), JSON.toJavaObject(unit,Unit.class));
            unitMaps.put(unit.getString("name"), unit.getLong("id"));
        }

        List<Unit> unitList = new ArrayList<>();

        // 构造单位对象
        for (String unitName : units) {
            if (unitName != null && !unitName.trim().isEmpty()) {
                String upperUnitName = unitName.toUpperCase();
                if (!existingUnits.containsKey(upperUnitName)) {
                    Unit unit = new Unit();
                    unit.setName(unitName);
                    unit.setEntityType(UnitentityTypeID);
                    unit.setActive(true);
                    unitList.add(unit);
                } else {
                    Unit existingUnit = existingUnits.get(upperUnitName);
                    unitMaps.put(unitName, existingUnit.getId());
                }
            }
        }

        if (unitList.isEmpty()) {
            logger.info("所有单位都已存在无需创建新单位");
            return;
        }

        try {
            BatchOperateResult insert = XObjectService.instance().insert(unitList, true,true);
            if (!insert.getSuccess()) {
                logger.error("单位创建失败: " + JSON.toJSONString(insert));
                for (OperateResult result : insert.getOperateResults()) {
                    if (!result.getSuccess()) {
                        logger.error("单位创建失败详情: " + JSON.toJSONString(result));
                        throw new CustomException("单位创建失败详情: " + result.getErrorMessage());
                    }
                }
                return;
            }

            List<Long> unitIds = insert.getOperateResults().stream()
                    .map(OperateResult::getDataId)
                    .collect(Collectors.toList());

            // 查询并更新单位映射
            if (!unitIds.isEmpty()) {
                String sql = "Select id,name from unit where id in (" + parseListToStr2(unitIds) + ")";
                QueryResult<JSONObject> query = XoqlService.instance().query(sql,true,true);
                query.getRecords().forEach(record -> {
                    unitMaps.put(record.getString("name"), record.getLong("id"));
                    logger.info("新建单位映射: " + record.getString("name") + " -> " + record.getLong("id"));
                });
            }
        } catch (Exception e) {
            logger.error("创建单位时发生错误", e);
            throw e;
        }
    }

    /**
     * 添加商品及相关数据
     * 处理流程：
     * 1. 检查重复商品
     * 2. 确保所需单位存在
     * 3. 创建商品基本信息
     * 4. 创建商品单位关系
     * 5. 创建产品信息
     *
     * @param reqProductInfoList 商品信息列表
     * @param response 响应对象，用于记录处理结果和错误信息
     * @throws ApiEntityServiceException 当API调用出错时抛出
     */
    public void addGoods(List<ReqProductInfo> reqProductInfoList, ResponseBody response) throws ApiEntityServiceException {
        if (reqProductInfoList == null || reqProductInfoList.isEmpty()) {
            logger.warn("输入的商品信息列表为空");
            return;
        }

        // 首先确保所有需要的单位都已经创建
        Set<String> requiredUnits = new HashSet<>();
        for (ReqProductInfo info : reqProductInfoList) {
            // 添加基本单位
            if (info.meins != null) {
                requiredUnits.add(info.meins);
            }
            // 添加其他单位
            if (info.units != null) {
                for (ProductUnitRow unit : info.units) {
                    if (unit.meinh != null) {
                        requiredUnits.add(unit.meinh);
                    }
                }
            }
        }

        // 查询有单位
        String sql ="Select id,name from unit where name in (" + parseListToStr2(new ArrayList<>(requiredUnits)) + ")";
        QueryResult<Unit> existingUnits = XObjectService.instance()
                .query(sql,true,true);
        // 更新单位映射
        for (Unit unit : existingUnits.getRecords()) {
            unitMaps.put(unit.getName(), unit.getId());
        }

        // 检查是否所有需要的单位都存在
        List<String> missingUnits = new ArrayList<>();
        for (String unit : requiredUnits) {
            if (!unitMaps.containsKey(unit)) {
                missingUnits.add(unit);
            }
        }

        // 如果有缺失的单位，先创建它们
        if (!missingUnits.isEmpty()) {
            logger.info("创建缺失的单位: " + missingUnits);
            try {
                GeneratingUnits(missingUnits, response);
            } catch (Exception e) {
                addError(response, "创建单位失败: " + e.getMessage());
            }
        }

        // 验证所有必需的单位是否都已经存在
        for (ReqProductInfo info : reqProductInfoList) {
            if (!unitMaps.containsKey(info.meins)) {
                logger.error("基本单位仍然缺失: " + info.meins);
                throw new CustomException("无法找到必需的基本单位: " + info.meins);
            }
        }

        // 开始创建商品
        List<Goods> goodsList = new ArrayList<>();
        List<Product> productList = new ArrayList<>();
        List<GoodsUnit> goodsUnitList = new ArrayList<>();

        try {
            // 获取必要的业务类型ID
            logger.info("获取到的业务类型ID - 商品: " + entityTypeID + ", 产品: " + productEntityTypeID + ", 商品单位: " + goodsUnitEntityTypeID);

            // 查找产品目录
            QueryResult<JSONObject> categoryQuery = XoqlService.instance()
                    .query("select id,name from productCategory where name = 'Product' ",true,true);
            if (categoryQuery.getRecords().isEmpty()) {
                logger.error("未找到产品目录");
                throw new CustomException("未找到产品目录");
            }
            Long categoryId = categoryQuery.getRecords().get(0).getLong("id");
            logger.info("获取到的产品目录ID: " + categoryId);

            // 构建商品数据
            for (ReqProductInfo reqProductInfo : reqProductInfoList) {
                if (reqProductInfo == null || reqProductInfo.salev == null) {
                    logger.warn("过无效的商品信息: " + JSON.toJSONString(reqProductInfo));
                    continue;
                }

                for (SalesInfoRow salesInfoRow : reqProductInfo.salev) {
                    // 创建商品
                    Goods goods = new Goods();
                    createGoods(goods, reqProductInfo, salesInfoRow, entityTypeID, categoryId, false, response);
//                    logger.info("准备创建商品: " + JSON.toJSONString(goods));
                    goodsList.add(goods);
                }
            }

            // 批量插入商品
            if (!goodsList.isEmpty()) {
                logger.info("开始创建 " + goodsList.size() + " 个商品");
                logger.info("本批次创建商品："+JSON.toJSONString(goodsList));
                BatchOperateResult goodsInsert = XObjectService.instance().insert(goodsList, true,true);
                if (!goodsInsert.getSuccess()) {
                    logger.error("商品创建失败: " + JSON.toJSONString(goodsInsert));
                    throw new CustomException("商品创建失败: " + goodsInsert.getErrorMessage());
//                    return;
                }

                //获取新创建的商品ID
                List<Long> goodsIds = goodsInsert.getOperateResults().stream()
                        .map(OperateResult::getDataId)
                        .collect(Collectors.toList());
                logger.info("成功创建商品，ID列表: " + goodsIds);

                // 查询新创建的商品
                QueryResult<JSONObject> newGoodsQuery = XoqlService.instance()
                        .query("select id,name from goods where id in (" + parseListToStr2(goodsIds) + ")",true,true);
                Map<String, Long> goodsIdMap = new HashMap<>();
                for (JSONObject record : newGoodsQuery.getRecords()) {
                    goodsIdMap.put(record.getString("name"),record.getLong("id"));
                }
                // 创建商品单位和产品
                for (ReqProductInfo reqProductInfo : reqProductInfoList) {
                    for (SalesInfoRow salesInfoRow : reqProductInfo.salev) {
//                        String goodsName = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                        String goodsName = null;
                        if(reqProductInfo.ltext_e != null){
                            goodsName = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                        }else if (reqProductInfo.ltext_1 != null){
                            goodsName = reqProductInfo.ltext_1 + "[" + salesInfoRow.vkorg + "]";
                        }else {
                            goodsName = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
                        }
                        Long goodsId = goodsIdMap.get(goodsName);
                        if (goodsId == null) {
                            logger.warn("未找到商品: " + goodsName);
                            continue;
                        }

                        // 创建商品单位和产品
                        createGoodsUnitsAndProducts(reqProductInfo,salesInfoRow, goodsName, goodsId,
                                productEntityTypeID, goodsUnitEntityTypeID, categoryId,
                                goodsUnitList, productList);
                    }
                }

                // 批量创建商品单位
                if (!goodsUnitList.isEmpty()) {
                    logger.info("开始创建 " + goodsUnitList.size() + " 个商品单位");
                    BatchOperateResult unitInsert = XObjectService.instance().insert(goodsUnitList, true,true);
                    if (!unitInsert.getSuccess()) {
                        logger.error("商品单位创建失败: " + JSON.toJSONString(unitInsert));
                        throw new CustomException("商品单位创建失败: " + unitInsert.getErrorMessage());
//                        return;
                    }
                    logger.info("商品单位创建成功");
                }

                // 批量创建产品
                if (!productList.isEmpty()) {
                    logger.info("开始创建 " + productList.size() + " 个产品");
                    BatchOperateResult productInsert = XObjectService.instance().insert(productList, true,true);
                    if (!productInsert.getSuccess()) {
                        logger.error("产品创建失败: " + JSON.toJSONString(productInsert));
                        throw new CustomException("产品创建失败: " + productInsert.getErrorMessage());
//                        return;
                    }
                    logger.info("产品创建成功");
                }
            } else {
                logger.warn("没有需要创建的商品");
            }

        } catch (Exception e) {
            logger.error("创建商品时发生错误: " + e.getMessage());
            throw e;
        }
    }

    private void createGoodsUnitsAndProducts(ReqProductInfo reqProductInfo,SalesInfoRow salesInfoRow, String goodsName, Long goodsId,
                                             Long productEntityTypeID, Long goodsUnitEntityTypeID, Long categoryId,
                                             List<GoodsUnit> goodsUnitList, List<Product> productList) {

        for (ProductUnitRow unit : reqProductInfo.units) {
            // 创建商品单位
            GoodsUnit goodsUnit = new GoodsUnit();
            goodsUnit.setUnit(unitMaps.get(unit.meinh));
            goodsUnit.setEntityType(goodsUnitEntityTypeID);
            Double umrez = String.valueOf(unit.umrez).isEmpty()  ? 0 : Double.valueOf(unit.umrez);
            Double umren = String.valueOf(unit.umren).isEmpty()  ? 0 : Double.valueOf(unit.umren);
            goodsUnit.setConversionRate(umrez/umren);
            goodsUnit.setGoods(goodsId);
            goodsUnitList.add(goodsUnit);
            logger.info("准备创建商品单位: " + JSON.toJSONString(goodsUnit));

            // 创建产品
            // hql更新10.产品只接受ST和默认的
			// 20250606 产品不创建了，在有Brand Name 接口后定时任务创建。
            /*if (Objects.equals(unit.meinh,"PC") || Objects.equals(unit.meinh,reqProductInfo.getSalev().get(0).getVrkme()) ){
                Product product = new Product();
                product.setExternal_Id__c(reqProductInfo.matnr+ "[" + salesInfoRow.vkorg + "]");
                product.setProductKey__c(reqProductInfo.matnr+"_"+salesInfoRow.vkorg+"_"+unit.meinh);
                product.setExternal_matnr__c(reqProductInfo.matnr);
                product.setPackUnit(unitMaps.get(unit.meinh));
                String ProductName = null;
                // 202503226 LJH uodate start
                //if (reqProductInfo.getMstae()!=null||reqProductInfo.getMstae()!=""){
                if (reqProductInfo.getMstae()!=null && reqProductInfo.getMstae()!=""){
                    //产品接口字段mstae空着的就是正常，只要有值就是冻结 hql修改
                    product.setEnableStatus(2);
                }else{
                    product.setEnableStatus(1); // ljh add
                }
                // 202503226 LJH uodate end
                if(reqProductInfo.ltext_e != null){
                    ProductName = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                }else if (reqProductInfo.ltext_1 != null){
                    ProductName = reqProductInfo.ltext_1 + "[" + salesInfoRow.vkorg + "]";
                }else {
                    ProductName = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
                }
                product.setProductName(ProductName+ "[" + salesInfoRow.vkorg + "]" + "[" + unit.meinh + "]");
                product.setEntityType(productEntityTypeID);
                product.setParentId(categoryId);
                product.setSkuType(unit.meinh.equals(reqProductInfo.meins) ? 1 : 2);
                product.setUnit(unit.meinh);
                product.setGoods(goodsId);
                product.setIndependentProduct(true);
                setOptionFieldValue2(product, "Sales_Organization__c", salesInfoRow.vkorg);
                productList.add(product);
                logger.info("准备创建产品: " + JSON.toJSONString(product));
            }*/
        }
    }

    /**
     * 更新商品及相关数据
     * @param goodsMap 需要更新的商品映射
     * @param requestParameters 请求参
     * @throws ApiEntityServiceException API调用出错时抛出
     */
    public void UPGoods(Map<String,Goods> goodsMap, List<ReqProductInfo> requestParameters, ResponseBody response) throws ApiEntityServiceException {
        if (goodsMap == null || goodsMap.isEmpty()) {
            logger.warn("没有需要更新的商品");
            return;
        }

        try {
            // 首先确保所有需要的单位都已经创建并加载到 unitMaps
            Set<String> requiredUnits = new HashSet<>();
            for (ReqProductInfo info : requestParameters) {
                // 添加基本单位
                if (info.meins != null) {
                    requiredUnits.add(info.meins);
                }
                // 添加其他单位
                if (info.units != null) {
                    for (ProductUnitRow unit : info.units) {
                        if (unit.meinh != null) {
                            requiredUnits.add(unit.meinh);
                        }
                    }
                }
            }

            // 查询现有单位
            String sql ="Select id,name from unit where name in (" + parseListToStr2(new ArrayList<>(requiredUnits)) + ")";
            QueryResult<JSONObject> existingUnits = XoqlService.instance()
                    .query(sql,true,true);

            // 更新单位映射
            for (JSONObject unit : existingUnits.getRecords()) {
                unitMaps.put(unit.getString("name"), unit.getLong("id"));
                logger.info("加载单位映射: " + unit.getString("name") + " -> " + unit.getLong("id"));
            }

            // 检查是否所有需要的单位都存在
            List<String> missingUnits = new ArrayList<>();
            for (String unit : requiredUnits) {
                if (!unitMaps.containsKey(unit)) {
                    missingUnits.add(unit);
                }
            }

            // 如果有缺失的单位，先创建它们
            if (!missingUnits.isEmpty()) {
                logger.info("创建缺失的单位: " + missingUnits);
                try {
                    GeneratingUnits(missingUnits, response);
                } catch (Exception e) {
                    addError(response, "创建单位失败: " + e.getMessage());
                }
            }

            // 验证所有必需的单位是否都已经存在
            for (ReqProductInfo info : requestParameters) {
                if (!unitMaps.containsKey(info.meins)) {
                    logger.error("基本单位仍然缺失: " + info.meins);
                    throw new IllegalStateException("无法找到必需的基本单位: " + info.meins);
                }
            }

            // 原有的更新逻辑
            List<Goods> goodsList = new ArrayList<>();
            List<Long> goodsIds = new ArrayList<>();
            List<Product> proList = new ArrayList<>();
            Map<String,Product> proMap = new HashMap<>();
            Map<String,List<GoodsUnit>> goodsUnitRowMap = new HashMap<>();
            List<GoodsUnit> upGoodsUnitList = new ArrayList<>();

            // 获取类型
//            Long productEntityTypeID = MetadataService.instance().getBusiType("product", "defaultBusiType").getId();
//            Long goodsUnitEntityTypeID = MetadataService.instance().getBusiType("goodsUnit", "defaultBusiType").getId();

            // 处理商品更新
            for (ReqProductInfo reqProductInfo : requestParameters) {
                for (SalesInfoRow salesInfoRow : reqProductInfo.salev) {
//                  String key = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                    String key = null;
                    if(reqProductInfo.ltext_e != null){
                        key = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                    }else if (reqProductInfo.ltext_1 != null){
                        key = reqProductInfo.ltext_1 + "[" + salesInfoRow.vkorg + "]";
                    }else {
                        key = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
                    }
					// 20250606 ljh 用External_matnr__c作为唯一商品标识 start 
					String keyNew = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
                    // Goods value = goodsMap.get(key);
					Goods value = goodsMap.get(keyNew);
					// 20250606 ljh 用External_matnr__c作为唯一商品标识 end 
                    if (value == null) {
                        continue;
                    }
                    createGoods(value, reqProductInfo, salesInfoRow, value.getEntityType(), value.getCategory(), true, response);
                    logger.info("更新商品：" + keyNew + " -> " + JSON.toJSONString(value));
                    goodsList.add(value);
                    goodsIds.add(value.getId());

                    // 处理商品单位
                    processProductUnits(reqProductInfo, key, value, productEntityTypeID,
                            goodsUnitEntityTypeID, proMap, goodsUnitRowMap,salesInfoRow);
                }
            }

            // 更新商品
            if (!goodsList.isEmpty()) {
                BatchOperateResult updateResult = XObjectService.instance().update(goodsList, true,true);
                if (!updateResult.getSuccess()) {
                    logger.error("更新商品失败: " + JSON.toJSONString(updateResult));
                    throw new CustomException("更新商品失败: " + updateResult.getErrorMessage());
//                    return;
                }
            }

            // 处理商品单位
            if (!goodsIds.isEmpty()) {
                processGoodsUnits(goodsIds, goodsUnitRowMap, upGoodsUnitList);
            }

            // 更新商品单位
            if (!upGoodsUnitList.isEmpty()) {
                BatchOperateResult unitUpdateResult = XObjectService.instance().update(upGoodsUnitList, true,true);
                if (!unitUpdateResult.getSuccess()) {
                    logger.error("更新商品单位失败: " + JSON.toJSONString(unitUpdateResult));
                    throw new CustomException("更新商品单位失败: " + unitUpdateResult.getErrorMessage());
//                    return;
                }
            }

            // 创建新产品
            if (!proMap.isEmpty()) {
                createNewProducts(proMap);
            }

        } catch (Exception e) {
            logger.error("更新商品时发生错误", e);
            throw e;
        }
    }

    /**
     * 处理产品单位信息
     * 功能：
     * 1. 查询现有商品单位
     * 2. 处理单位转换关系
     * 3. 创建或更新商品单位
     * 4. 创建对应的产品记录
     *
     * @param reqProductInfo 商品信息对象
     * @param key 商品标识键
     * @param value 商品对象
     * @param productEntityTypeID 产品实体类型ID
     * @param goodsUnitEntityTypeID 商品单位实体类型ID
     * @param proMap 产品映射，用于存储需要创建的产品
     * @param goodsUnitRowMap 商品单位映射，用于存储需要处理的单位关系
     */
    private void processProductUnits(ReqProductInfo reqProductInfo, String key, Goods value,
                                     Long productEntityTypeID, Long goodsUnitEntityTypeID,
                                     Map<String,Product> proMap, Map<String,List<GoodsUnit>> goodsUnitRowMap,SalesInfoRow salesInfoRow) {

        try {
            // 先查询现有商品单位
            QueryResult<JSONObject> existingUnits = XoqlService.instance().query(
                    "select id, unit.id as unitId from goodsUnit where goods = " + value.getId(),true,true);
            Set<Long> existingUnitIds = existingUnits.getRecords().stream()
                    .map(record -> record.getLong("unitId"))
                    .collect(Collectors.toSet());

            for (ProductUnitRow unit : reqProductInfo.units) {
                Long unitId = unitMaps.get(unit.meinh);

                if (unitId == null) {
                    logger.error("找不到单位ID: " + unit.meinh);
                    continue;
                }

                // 创建商品单位
                GoodsUnit goodsUnit = new GoodsUnit();
                goodsUnit.setUnit(unitId);
                goodsUnit.setEntityType(goodsUnitEntityTypeID);
                Double umrez = String.valueOf(unit.umrez).isEmpty()  ? 0 : Double.valueOf(unit.umrez);
                Double umren = String.valueOf(unit.umren).isEmpty()  ? 0 : Double.valueOf(unit.umren);
                goodsUnit.setConversionRate(umrez/umren);
                goodsUnit.setGoods(value.getId());

                String unitKey = key + "[" + unitId + "]";
                logger.info("unitKey:"+unitKey);
                List<GoodsUnit> unitList = goodsUnitRowMap.computeIfAbsent(unitKey, k -> new ArrayList<>());
                logger.info("unitList:"+unitList);
                // 检查是否已经添加过这个单位
                boolean exists = false;
                for (GoodsUnit existing : unitList) {
                    if (existing.getUnit().equals(unitId)) {
                        exists = true;
                        // 更新转换率
                        existing.setConversionRate(umrez/umren);
                        break;
                    }
                }

                if (!exists) {
                    unitList.add(goodsUnit);

                    // 只有在商品单位不存在时才创建对应的产品
                    if (Objects.equals(unit.meinh,"PC") || Objects.equals(unit.meinh,reqProductInfo.getSalev().get(0).getVrkme()) ) {
                        if (!existingUnitIds.contains(unitId)) {
                            String productName = key + "[" + unit.meinh + "]";
                            Product pro = new Product();
                            pro.setExternal_Id__c(reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]");
                            pro.setProductKey__c(reqProductInfo.matnr + "_" + salesInfoRow.vkorg + "_" + unit.meinh);
                            pro.setExternal_matnr__c(reqProductInfo.matnr);
                            pro.setPackUnit(unitId);
                            if (reqProductInfo.getMstae()!=null && reqProductInfo.getMstae()!=""){
                                //产品接口字段mstae空着的就是正常，只要有值就是冻结 hql修改
                                pro.setEnableStatus(2);
                            }else{
                                pro.setEnableStatus(1); // ljh add
                            }
                            pro.setProductName(productName);
                            pro.setEntityType(productEntityTypeID);
                            pro.setParentId(value.getCategory());
                            pro.setSkuType(unit.meinh.equals(reqProductInfo.meins) ? 1 : 2);
                            pro.setUnit(unit.meinh);
                            pro.setGoods(value.getId());
                            pro.setIndependentProduct(true);
                            setOptionFieldValue2(pro, "Sales_Organization__c", salesInfoRow.vkorg);
                            proMap.put(pro.getProductName(), pro);
                            logger.info("准备创建新产品: " + JSON.toJSONString(pro));
                        }
                    }
                }

                logger.info("处理商品单位: " + key + " -> 单位: " + unit.meinh +
                        ", ID: " + unitId + ", 转换率: " + unit.umren);
            }
        } catch (ApiEntityServiceException e) {
            logger.error("处理商品单位时发生错误: " + e.getMessage());
            throw new CustomException("处理商品单位时发生错误: " + e.getMessage());

        }
    }

    /**
     * 处理商品单位数据
     * 功能：
     * 1. 查询并更新现有商品单位
     * 2. 创建新的商品单位关系
     * 3. 处理单位转换率
     *
     * @param goodsIds 商品ID列表
     * @param goodsUnitRowMap 商品单位映射
     * @param upGoodsUnitList 需要更新的商品单位列表
     * @throws ApiEntityServiceException 当API调用出错时抛出
     */
    private void processGoodsUnits(List<Long> goodsIds,
                                   Map<String, List<GoodsUnit>> goodsUnitRowMap,
                                   List<GoodsUnit> upGoodsUnitList) throws ApiEntityServiceException {
        try {
            // 查询现有的商品单位和商品基础单位
            Map<String, GoodsUnit> existingUnitMap = new HashMap<>();
            Map<Long, Long> goodsBaseUnitMap = new HashMap<>(); // 存储商品ID到基础单位ID的映射

            // 首先查询商品的基础单位
            QueryResult<JSONObject> goodsQuery = XoqlService.instance().query(
                    "select id, baseUnit from goods where id in (" + parseListToStr2(goodsIds) + ")",true,true);
            for (JSONObject record : goodsQuery.getRecords()) {
                goodsBaseUnitMap.put(record.getLong("id"), record.getLong("baseUnit"));
            }

            // 查询现有的商品单位
            QueryResult<JSONObject> query = XoqlService.instance().query(
                    "select id, name, goods.name AS gname, unit.name AS uname, unit.id AS uid, " +
                            "goods.id AS goodsId, conversionRate from goodsUnit where goods in (" +
                            parseListToStr2(goodsIds) + ")",true,true);

            // 构建现有单位映射
            for (JSONObject record : query.getRecords()) {
                String key = record.getLong("goodsId") + "_" + record.getLong("uid");

                GoodsUnit existingUnit = new GoodsUnit();
                existingUnit.setId(record.getLong("id"));
                existingUnit.setUnit(record.getLong("uid"));
                existingUnit.setConversionRate(record.getDouble("conversionRate"));
                existingUnitMap.put(key, existingUnit);

                logger.info("现有商品单位: " + record.getString("gname") + " -> " +
                        record.getString("uname") + " (转换率: " + record.getDouble("conversionRate") + ")");
            }

            // 处理新增和更新的商品单位
            List<GoodsUnit> newGoodsUnits = new ArrayList<>();
            for (Map.Entry<String, List<GoodsUnit>> entry : goodsUnitRowMap.entrySet()) {
                List<GoodsUnit> units = entry.getValue();

                for (GoodsUnit goodsUnit : units) {
                    if (goodsUnit.getUnit() == null || goodsUnit.getGoods() == null) {
                        logger.error("商品单位的单位ID或商品ID为空");
                        continue;
                    }

                    String key = goodsUnit.getGoods() + "_" + goodsUnit.getUnit();
                    GoodsUnit existingUnit = existingUnitMap.get(key);

                    if (existingUnit != null) {
                        // 直接更新现有单位
                        GoodsUnit updateUnit = new GoodsUnit();
                        updateUnit.setId(existingUnit.getId());
                        updateUnit.setConversionRate(goodsUnit.getConversionRate());
                        upGoodsUnitList.add(updateUnit);
                        logger.info("更新商品单位: 商品ID=" + goodsUnit.getGoods() +
                                ", 单位ID=" + goodsUnit.getUnit() +
                                ", 新转换率=" + goodsUnit.getConversionRate());
                    } else {
                        // 这个商品和单位的组合不存在，需要创建
                        newGoodsUnits.add(goodsUnit);
                        logger.info("新增商品单位: 商品ID=" + goodsUnit.getGoods() +
                                ", 单位ID=" + goodsUnit.getUnit() +
                                ", 转换率=" + goodsUnit.getConversionRate());
                    }
                }
            }

            // 创建新的商品单位
            if (!newGoodsUnits.isEmpty()) {
                logger.info("准备创建 " + newGoodsUnits.size() + " 个新商品单位");
                BatchOperateResult insertResult = XObjectService.instance().insert(newGoodsUnits, true,true);
                if (!insertResult.getSuccess()) {
                    logger.error("创建商品单位失败: " + JSON.toJSONString(insertResult));
                    throw new CustomException("创建商品单位失败: " + insertResult.getErrorMessage());
//                    return;
                }
                logger.info("成功创建新商品单位");
            }

            // 更新现有商品单位
            if (!upGoodsUnitList.isEmpty()) {
                logger.info("准备更新 " + upGoodsUnitList.size() + " 个商品单位");
                BatchOperateResult updateResult = XObjectService.instance().update(upGoodsUnitList, true,true);
                if (!updateResult.getSuccess()) {
                    logger.error("更新商品单位失败: " + JSON.toJSONString(updateResult));
                    throw new CustomException("更新商品单位失败: " + updateResult.getErrorMessage());
//                    return;
                }
            }

        } catch (Exception e) {
            logger.error("处理商品单位时发生错误: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 创建新产品
     * 功能：批量创建产品记录
     *
     * @param proMap 产品映射，key为产品名称，value为产品对象
     * @throws ApiEntityServiceException 当API调用出错时抛出
     */
    private void createNewProducts(Map<String,Product> proMap) throws ApiEntityServiceException {
        if (proMap == null || proMap.isEmpty()) {
            logger.info("没有需要创建的品");
            return;
        }
        List<Product> productsToCreate = new ArrayList<>(proMap.values());
        logger.info("准备创建产品列表: " + JSON.toJSONString(productsToCreate));

        BatchOperateResult insertResult = XObjectService.instance().insert(productsToCreate, true,true);
        if (!insertResult.getSuccess()) {
            logger.error("创建产品失败: " + JSON.toJSONString(insertResult));
            // 记录详细的错误信息
            for (OperateResult result : insertResult.getOperateResults()) {
                if (!result.getSuccess()) {
                    logger.error("产品创建失败详情: " + JSON.toJSONString(result));
                    throw new CustomException("产品创建失败详情: " + result.getErrorMessage());
                }
            }
        } else {
            logger.info("成功创建 " + productsToCreate.size() + " 个产品");
        }
    }


    public static void main(String[] args) throws ApiEntityServiceException, IOException {
        // 删除或注释掉测试代码
        String json = "{\"resultinfo\":[{\"ltext_e\":\"Emamectin Benzoate 5% WG 10g/B22\",\"gewei\":\"KG\",\"matnr\":\"0000002003010021432\",\"units\":[{\"meinh\":\"ST\",\"umren\":100,\"meins\":\"KG\",\"umrez\":1},{\"meinh\":\"KG\",\"umren\":1,\"meins\":\"KG\",\"umrez\":1},{\"meinh\":\"G\",\"umren\":1000,\"meins\":\"KG\",\"umrez\":1},{\"meinh\":\"EA\",\"umren\":100,\"meins\":\"KG\",\"umrez\":1}],\"matkl\":\"200301\",\"salev\":[{\"vrkme\":\"EA\",\"ktgrm\":\"01\",\"mtpos\":\"NORM\",\"vkorg\":\"7020\",\"vtweg\":\"00\"}],\"spart\":\"10\",\"labor\":\"002\",\"werks\":\"7020\",\"ltext_1\":\"K32 5% WG 10g/B\",\"brgew\":1.000,\"extwg\":\"K32\",\"ltext_s\":\"K32 5% WG 10g/B\",\"ntgew\":1.000,\"meins\":\"KG\"}],\"esbinfo\":{\"instid\":\"GMM031\",\"requesttime\":\"2025-04-17 07:00:01.000\",\"attr2\":\"NEOCRM\",\"attr1\":\"SAP\"}}";
        SAPProductWebService sapProductWebService = new SAPProductWebService();
        sapProductWebService.doPost(JSONObject.parseObject(json));
    }

    /**
     * 创建或更新商品基本信息
     * @param goods 商品对象
     * @param reqProductInfo 商品请求信息
     * @param salesInfoRow 销售信息
     * @param entityTypeID 实体类型ID
     * @param categoryId 分类ID
     * @param isUpdate 是否是更新操作
     */
    private void createGoods(Goods goods, ReqProductInfo reqProductInfo, SalesInfoRow salesInfoRow,
                             Long entityTypeID, Long categoryId, boolean isUpdate, ResponseBody response) {
        try {
            if (!isUpdate) {
                // 只在新建时设置这些基本信息
//                String goodsName = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
//                String goodsName = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                String goodsName = null;
                if(reqProductInfo.ltext_e != null){
                    goodsName = reqProductInfo.ltext_e + "[" + salesInfoRow.vkorg + "]";
                }else if (reqProductInfo.ltext_1 != null){
                    goodsName = reqProductInfo.ltext_1 + "[" + salesInfoRow.vkorg + "]";
                }else {
                    goodsName = reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]";
                }
                goods.setName(goodsName);
                goods.setExternal_matnr__c(reqProductInfo.matnr + "[" + salesInfoRow.vkorg + "]");
                goods.setExternal_Id__c(reqProductInfo.matnr);
                // 检查并设置基本单位
                Long baseUnitId = unitMaps.get(reqProductInfo.meins);
                if (baseUnitId == null) {
                    String error = "找不到基本单位: " + reqProductInfo.meins;
                    logger.error(error);
                    addError(response, error);
                    throw new IllegalStateException(error);
                }
                goods.setBaseUnit(baseUnitId);

                goods.setCategory(categoryId);
                goods.setEntityType(entityTypeID);
                goods.setMultiUnit(true);
            }

            // 这些字段在新建和更新时都需要设置
            // 描述信息
            goods.setChinese_Description__c(reqProductInfo.ltext_1);
            goods.setEnglish_Description__c(reqProductInfo.ltext_e);
            goods.setSpanish_Description__c(reqProductInfo.ltext_es);
            goods.setPortuguese_Description__c(reqProductInfo.ltext_pt);
            // 状态信息
            goods.setCross_Factory_Material_Status__c(reqProductInfo.mstae);
            goods.setEffective_Start__c(reqProductInfo.mstde);
            // 其他信息
            goods.setExternal_Material_Group__c(reqProductInfo.extwg);
            goods.setIndustry_Standard_Description__c(reqProductInfo.normt);
            goods.setGross_Weight__c(reqProductInfo.brgew);
            goods.setNet_Weight__c(reqProductInfo.brgew);
            goods.setBusiness_Volume__c(reqProductInfo.volum);
            //处理选项字段
            setOptionFieldValue(goods, "Basic_Metering_Unit__c", reqProductInfo.meins);
            setOptionFieldValue(goods, "Division__c", reqProductInfo.spart);
            setOptionFieldValue(goods, "Laboratory_Office__c", reqProductInfo.labor);
            setOptionFieldValue(goods, "material_Group__c", reqProductInfo.matkl);
            setOptionFieldValue(goods, "Material_Type__c", reqProductInfo.mtart);
            setOptionFieldValue(goods, "Volumetric_Unit__c", reqProductInfo.voleh);
            setOptionFieldValue(goods, "Weight_Unit__c", reqProductInfo.gewei);
            setOptionFieldValue(goods, "Delivering_Plant__c", salesInfoRow.dwerk);
            setOptionFieldValue(goods, "distr_Channel__c", salesInfoRow.vtweg);
            setOptionFieldValue(goods, "material_Subject_Assign_Group__c", salesInfoRow.ktgrm);
            setOptionFieldValue(goods, "factory__c", reqProductInfo.werks);
            setOptionFieldValue(goods, "Project_Category_Group__c", salesInfoRow.mtpos);
            setOptionFieldValue(goods, "Sales_Organization__c", salesInfoRow.vkorg);
            setOptionFieldValue(goods,"Default_Sales_Unit__c",salesInfoRow.vrkme);
        } catch (Exception e) {
            String error = "创建商品信息时发生错误: " + e.getMessage();
            logger.error(error);
            addError(response, error);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * 安全设置选项字段值
     */
    private void setOptionFieldValue(Goods goods, String fieldName, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                logger.info("字段 " + fieldName + " 的值为空");
                return;
            }

            Map<String, Integer> fieldOptions = allOptions.get(fieldName);
            if (fieldOptions == null) {
                logger.warn("未找到字段 " + fieldName + " 的选项值映射");
                return;
            }

            Integer optionValue = fieldOptions.get(value);
            if (optionValue == null) {
                logger.warn("字段 " + fieldName + " 未找到值 " + value + " 的映射");
                return;
            }

            // 使用反射设置字段值
            try {
                // String setterName = "set" + fieldName;
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                goods.getClass().getMethod(setterName, Integer.class).invoke(goods, optionValue);
                logger.info("设置字段 " + fieldName + " 的值: " + value + " -> " + optionValue);
            } catch (Exception e) {
                logger.error("设置字段 " + fieldName + " 值时发生错误: " + e.getMessage());
                throw new CustomException("设置字段 " + fieldName + " 值时发生错误: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("处理字段 " + fieldName + " 时发生错误: " + e.getMessage());
            throw new CustomException("处理字段 " + fieldName + " 时发生错误: " + e.getMessage());
        }
    }

    /**
     * 安全设置选项字段值
     */
    private void setOptionFieldValue2(Product goods, String fieldName, String value) {
        logger.info(value);
        try {
            if (value == null || value.trim().isEmpty()) {
                logger.info("字段 " + fieldName + " 的值为空");
                return;
            }

            Map<String, Integer> fieldOptions = allOptionsP.get(fieldName);
            if (fieldOptions == null) {
                logger.warn("未找到字段 " + fieldName + " 的选项值映射");
                return;
            }

            Integer optionValue = fieldOptions.get(value);
            if (optionValue == null) {
                logger.warn("字段 " + fieldName + " 未找到值 " + value + " 的映射");
                return;
            }

            // 使用反射设置字段值
            try {
                String setterName = "set" + fieldName;
                goods.getClass().getMethod(setterName, Integer.class).invoke(goods, optionValue);
                logger.info("设置字段 " + fieldName + " 的值: " + value + " -> " + optionValue);
            } catch (Exception e) {
                logger.error("设置字段 " + fieldName + " 值时发生错误: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("处理字段 " + fieldName + " 时发生错误: " + e.getMessage());
        }
    }

}
