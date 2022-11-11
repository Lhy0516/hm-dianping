package com.hmdp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopMapper shopMapper;

    /**
     * IPage的实例初始化时将size设置为复数，则分页失效。records里是所有数据
     */
    @Test
    public void test1(){
        IPage<Shop> page = new Page<>();
        page.setSize(-1L);
        IPage<Shop> shopIPage = shopMapper.pageShop(page);
        shopIPage.getRecords().forEach(System.out::println);
        page = new Page<>(1,-1L);
        System.out.println(shopMapper.pageShop(page).getRecords().size());
    }

    @Test
    public void test2(){
        //pege传null部分也，正常返回List
        IPage<Shop> page = null;
        List<Shop> shopList = shopMapper.pageShop2(page);
        shopList.forEach(System.out::println);

        //pege不为null时，除了records需要手动set外，其余page信息都有
        page = new Page<>(1,10);
        List<Shop> pageShop2 = shopMapper.pageShop2(page);
        System.out.println(pageShop2.size());//10
        System.out.println(page.getTotal());//14
    }

}
