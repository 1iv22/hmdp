package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        // 1.从 Redis 中查询商铺缓存
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);

        // 2.判断 Redis 中是否有该缓存
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            // 2.1.若 Redis 中存在该缓存，则直接返回
            ArrayList<ShopType> typeList = new ArrayList<>();
            for (String str : shopTypeJsonList) {
                typeList.add(JSONUtil.toBean(str, ShopType.class));
            }
            //也可以用下面的stream流的方式，其实大差不差，都是遍历，每个数据都转换类型后再操作
//            List typeList = shopTypeJsonList.stream().map((shopTypeJson)->{
//                ShopType shopType = JSONUtil.toBean(shopTypeJson, ShopType.class);
//                return shopType;
//            }).collect(Collectors.toList());
            return Result.ok(typeList);
        }
        // 2.2.Redis 中若不存在该数据，则从数据库中查询
        List<ShopType> typeList = query().orderByAsc("sort").list();

        // 3.判断数据库中是否存在
        if (typeList == null || typeList.isEmpty()) {
            // 3.1.数据库中也不存在，则返回 false
            return Result.fail("分类不存在！");
        }

        // 3.2数据库中存在，则将查询到的信息存入 Redis
        for (ShopType shopType : typeList) {
            stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopType));
        }
        //下面是stream流的方式
//        List shopTypeJson = typeList.stream().map((shopType)-> {
//            String jsonStr = JSONUtil.toJsonStr(shopType);
//            return jsonStr;
//        }).collect(Collectors.toList());
//        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_LIST_KEY,shopTypeJson);

        // 3.3返回
        return Result.ok(typeList);
    }
}
