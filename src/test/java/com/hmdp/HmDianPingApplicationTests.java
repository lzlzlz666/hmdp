package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private IShopService ishopService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testSaveShop() {
        // 这里的 1L 是你要测试的店铺ID
        // 这里的 10L 是逻辑过期时间（秒），测试时可以设短一点或长一点
        saveShop2Redis(1L, 10L);

        System.out.println("预热成功！数据已写入 Redis");
    }

    public void saveShop2Redis(Long id, Long expireSeconds) {
        // 1. 查询店铺数据
        Shop shop = ishopService.getById(id);
        // 2. 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        // 重点：这里设置了过期时间，就是为了解决你那个报错
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3. 写入 Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Test
    void loadShopData() {
        // 1. 查询店铺信息
        List<Shop> list = ishopService.list();

        // 2. 把店铺分组，按照 typeId 分组，typeId 一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));

        // 3. 分批完成写入 Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {

            // 3.1 获取类型 id
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;

            // 3.2 获取同类型的店铺集合
            List<Shop> value = entry.getValue();

            List<RedisGeoCommands.GeoLocation<String>> locations =
                    new ArrayList<>(value.size());

            // 3.3 写入 redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {

                // 单条写法（已注释）
                // stringRedisTemplate.opsForGeo()
                //     .add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());

                // 批量写法
                locations.add(
                        new RedisGeoCommands.GeoLocation<>(
                                shop.getId().toString(),
                                new Point(shop.getX(), shop.getY())
                        )
                );
            }

            // ⚠️ 真正批量写入 Redis（你截图下面一般会有这一行）
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

}
