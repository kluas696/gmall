package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuPosterMapper spuPosterMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RabbitService rabbitService;

    //??????????????????
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }
    //????????????category1Id??????????????????
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        System.out.println("=========");
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
        return baseCategory2List;
    }

    //????????????category2Id??????????????????
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
        return baseCategory3List;
    }

    //??????????????????
    @Override
    public List<BaseAttrInfo> getArrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectBaseArrInfoList(category1Id,category2Id,category3Id);
        return baseAttrInfoList;
    }

    //??????????????????
    @Override
    @Transactional(rollbackFor = Exception.class)//???????????????????????????
    public void saveArrInfo(BaseAttrInfo baseAttrInfo) {
        /**
         * ?????? baseAttrInfo.getId();
         * ?????????????????????insert,
         * ??????????????????????????????,
         * ???????????? base_attr_info ???
         */
        if (baseAttrInfo.getId() != null){
            //??????
            baseAttrInfoMapper.updateById(baseAttrInfo);

            //????????????????????????????????? base_attr_value ???
            //?????????
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("attr_id",baseAttrInfo.getId());
            baseAttrValueMapper.delete(queryWrapper);
        }else{
        //????????????
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

        //?????????????????????????????????????????? base_attr_value ???
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //???????????????
        if (!CollectionUtils.isEmpty(attrValueList)){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);

            }
        }

    }
    //????????????id,???????????????????????????
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",attrId);
        return baseAttrValueMapper.selectList(queryWrapper);

    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //???????????????
        if (baseAttrInfo != null){
            //??????????????????????????????????????????baseAttrInfo
            baseAttrInfo.setAttrValueList(this.getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    /**
     * ????????????????????????????????????
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {

        //????????????
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        queryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPage,queryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectList(null);
        return baseSaleAttrList;

    }

    //??????????????????
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        //spuInfo
        //spuImage
        //spuSaleAttr
        //spuSaleAttrValue
        //spuPoster

        //????????????????????????
        spuInfoMapper.insert(spuInfo);

        //??????
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
        //????????????
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //???????????????
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.stream().forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        //??????????????????
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            }

        }
        //??????
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)){
            for (SpuPoster spuPoster : spuPosterList) {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            }

        }


    }
    //??????spuImage??????
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(wrapper);
    }

    //??????spuId????????????????????????(???????????????????????????)
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);

    }
    //??????skuInfo
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {

        //skuInfo
        skuInfoMapper.insert(skuInfo);

        //skuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.stream().forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        //skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
        //skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.stream().forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }


    }

    /**
     * ??????skuInfo ??????
     * @param skuInfoPage
     * @return
     */
    @Override
    public IPage<SkuInfo> getSkuInfoPage(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,wrapper);
    }
    //??????
    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        //????????????
        //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);

    }
    //??????
    @Override
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);

        //????????????
        //rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
        }

    //???service-item??????????????????

    @Override
    @GmallCache(prefix = "getSkuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {


        return getSkuInfoDB(skuId);
    }

    /**
     * ?????????????????? redisson
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        //??????????????????key skuKey=sku:skuId:info
        String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        try {
            //??????key??????value
            SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {
                //????????????????????????
                if (skuKey != null) {
                    //?????????????????? lockKey=sku:skuId:lock
                    String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                    //redisson
                    RLock lock = redissonClient.getLock(lockKey);
                    //??????
                    boolean b = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                    if (b) {
                        try {
                            //????????????
                            //???????????????
                            skuInfo = getSkuInfoDB(skuId);
                            //?????????
                            if (skuInfo == null) {
                                //??????????????????????????????null,?????????????????????
                                SkuInfo skuInfo1 = new SkuInfo();
                                this.redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                                return skuInfo1;
                            }
                            //????????????
                            this.redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo;
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            //??????
                            lock.unlock();
                        }
                    } else {
                        //????????????
                        Thread.sleep(100);
                        return getSkuInfo(skuId);
                    }
                } else {
                    //??????????????????
                    return skuInfo;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }

    /**
     * ?????????????????? Redis
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedis(Long skuId) {
        //??????????????????key skuKey=sku:skuId:info
        String skuKey = RedisConst.SKUKEY_PREFIX+ skuId + RedisConst.SKUKEY_SUFFIX;
        //??????key??????value
        SkuInfo skuInfo= (SkuInfo) redisTemplate.opsForValue().get(skuKey);

        try {
            //??????skuInfo????????????,???????????????????????????
            if (skuInfo == null) {
                //????????????????????????
                if (skuKey != null) {
                    //?????????????????? lockKey=sku:skuId:lock
                    String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                    //??????token?????????
                    String token = UUID.randomUUID().toString();
                    //?????????????????????
                    Boolean aBoolean = this.redisTemplate.opsForValue().setIfAbsent(lockKey, token, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                    if (aBoolean) {
                        //???????????????
                        skuInfo = getSkuInfoDB(skuId);
                        //?????????
                        if (skuInfo == null) {
                            //??????????????????????????????null,?????????????????????
                            SkuInfo skuInfo1 = new SkuInfo();
                            this.redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //????????????
                        this.redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        //??????????????????
                        //  ????????????lua ??????
                        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                        //  ????????????lua ??????
                        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                        //  ???lua????????????DefaultRedisScript ?????????
                        redisScript.setScriptText(script);
                        //  ??????DefaultRedisScript ?????????????????????
                        redisScript.setResultType(Long.class);
                        //  ????????????
                        redisTemplate.execute(redisScript, Arrays.asList("lockKey"), token);
                        return skuInfo;
                    } else {
                        //???????????????
                        Thread.sleep(100);
                        return getSkuInfo(skuId);
                    }
                } else {
                    //???skuInfo??????null
                    return skuInfo;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }


    /**
     * ?????????????????????
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //??????skuInfo????????????
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //??????skuId??????skuImage????????????
        QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(wrapper);
        if (skuInfo!=null){
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {

        //?????????????????????
        RLock lock = redissonClient.getLock(skuId + "lock");
        lock.lock();

        try {
            QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("id",skuId);
            //??????????????????
            wrapper.select("price");
            SkuInfo skuInfo = skuInfoMapper.selectOne(wrapper);
            if (skuInfo != null){
                return skuInfo.getPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return new BigDecimal(0);
    }
    //??????????????????
    @Override
    @GmallCache(prefix = "getCategoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {

        return baseCategoryViewMapper.selectById(category3Id);
    }
    //?????????????????????????????????????????????
    @Override
    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);

    }

    @Override
    @GmallCache(prefix = "getSkuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {

        HashMap<Object, Object> hashMap = new HashMap<>();

        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdsMapBySpuId(spuId);
        if (!CollectionUtils.isEmpty(mapList)){
            for (Map map : mapList) {
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }

        return hashMap;
    }
    //??????????????????
    @Override
    @GmallCache(prefix = "getSkuPosterBySpuId:")
    public List<SpuPoster> getSkuPosterBySpuId(long spuId) {
        return spuPosterMapper.selectList(new QueryWrapper<SpuPoster>().eq("spu_id",spuId));
    }

    @Override
    @GmallCache(prefix = "getAttrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectAttrList(skuId);
    }

    /**
     * ????????????????????????
     * @return
     */
    @Override
    @GmallCache(prefix ="getCategoryList:" )
    public List<JSONObject> getCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        //????????????????????????????????????
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //??????????????????id????????????,?????????????????????????????????
        Map<Long, List<BaseCategoryView>> category1Map =
                baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //??????????????????id
            Long category1Id = entry1.getKey();
            //????????????id?????????????????????
            List<BaseCategoryView> categoryViewList1 = entry1.getValue();
            //?????????????????????????????????????????????????????????
            JSONObject category1 = new JSONObject();
            //????????????id
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            //??????????????????
            category1.put("categoryName",categoryViewList1.get(0).getCategory1Name());

            index++;

            //???????????????????????????
            Map<Long, List<BaseCategoryView>> category2Map = categoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //??????????????????????????????????????????
            List<JSONObject> categoryChild2List = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //??????????????????id
                Long category2Id = entry2.getKey();
                //??????????????????
                List<BaseCategoryView> categoryViewList2 = entry2.getValue();
                //?????????????????????????????????????????????????????????
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",categoryViewList2.get(0).getCategory2Name());

                categoryChild2List.add(category2);


                //??????????????????
                //??????????????????????????????????????????
                List<JSONObject> categoryChild3List = new ArrayList<>();
                categoryViewList2.forEach(baseCategoryView -> {
                    //?????????????????????????????????????????????????????????
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",baseCategoryView.getCategory3Id());
                    category3.put("categoryName",baseCategoryView.getCategory3Name());
                    categoryChild3List.add(category3);
                });
                //????????????????????????
                category2.put("categoryChild",categoryChild3List);
            }
            //?????????????????????????????????
            category1.put("categoryChild",categoryChild2List);
            list.add(category1);
        }

        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }


}
