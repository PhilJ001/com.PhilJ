package com.PhilJ.goods.service.impl;

import com.PhilJ.goods.dao.*;
import com.PhilJ.goods.pojo.*;
import com.PhilJ.goods.service.SpuService;
import com.PhilJ.util.IdWorker;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Spu findById(String id){
        return  spuMapper.selectByPrimaryKey(id);
    }


    /**
     * 增加
     * @param goods
     */
    @Override
    @Transactional
    public void add(Goods goods){
        //1.添加spu
        Spu spu = goods.getSpu();
        //1.1设置分布式id
        long spuId = idWorker.nextId();
        String id = String.valueOf(spuId);
        //设置 spu 的属性参数
        spu.setId(id);
        //设置删除状态
        spu.setIsDelete("0");
        //设置上架状态
        spu.setIsMarketable("0");
        //审核状态
        spu.setStatus("0");
        //添加spu
        spuMapper.insertSelective(spu);

        //添加Sku集合
        this.saveSkuList(goods);

    }

    //向数据库中添加sku数据
    private void saveSkuList(Goods goods) {
        //获取spu
        Spu spu = goods.getSpu();
        //获取商品分类id
        //通过spu获取category3Id
        Integer category3Id = spu.getCategory3Id();
        //根据category3Id查询商品分类对象
        Category category = categoryMapper.selectByPrimaryKey(category3Id);

        //获取spu中品牌id
        Integer brandId = spu.getBrandId();
        //根据品牌id查询品牌对象
        Brand brand = brandMapper.selectByPrimaryKey(brandId);

        //设置品牌与分类的关联关系
        //查询关联表
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setBrandId(brandId);
        categoryBrand.setCategoryId(category3Id);
        int count = categoryBrandMapper.selectCount(categoryBrand);
        if (count == 0){
            //brandID没和CategoryID关联，需要绑定
            categoryBrandMapper.insert(categoryBrand);
        }

        //获取sku集合
        List<Sku> skuList = goods.getSkuList();
        if (skuList != null){
            //遍历sku集合,循环填充数据并添加到数据库中
            for (Sku sku : skuList) {
                //设置skuId
                sku.setId(String.valueOf(idWorker.nextId()));
                //设置sku规格数据
                //判断规格数据是否为空，如果是，传入个空json
                if (StringUtils.isEmpty(sku.getSpec())){
                    sku.setSpec("{}");
                }
                //设置sku名称(spu名称+规格)
                String skuname = spu.getName();
                //因为规格信息是json字符串格式，所以需要转为Map集合
                Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
                if (specMap != null && specMap.size() > 0){
                    //遍历Map中value值(规格信息)，拼接sku名称
                    for (String value : specMap.values()) {
                        //spu名称+规格
                        skuname+=" "+value;                    }
                }
                sku.setName(skuname);
                //设置spuid
                sku.setSpuId(spu.getId());
                //设置创建与修改时间
                sku.setCreateTime(new Date());
                sku.setUpdateTime(new Date());
                //商品分类id
                sku.setCategoryId(category.getId());
                //设置商品分类名称
                sku.setCategoryName(category.getName());
                //设置品牌名称
                sku.setBrandName(brand.getName());
                //将sku添加到数据库
                skuMapper.insertSelective(sku);
            }
        }
    }


    /**
     * 修改
     * @param goods
     */
    @Override
    @Transactional
    public void update(Goods goods){
        //修改spu
        Spu spu = goods.getSpu();
        spuMapper.updateByPrimaryKey(spu);

        //先删除原先的sku
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",spu.getId());
        skuMapper.deleteByExample(example);

        //再重新添加新的sku
        this.saveSkuList(goods);

    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(String id){
        spuMapper.deleteByPrimaryKey(id);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Spu> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Spu> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Spu>)spuMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Spu> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Spu>)spuMapper.selectByExample(example);
    }

    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id",searchMap.get("id"));
           	}
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andEqualTo("sn",searchMap.get("sn"));
           	}
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
           	}
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
           	}
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
           	}
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
           	}
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
           	}
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
           	}
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
           	}
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
           	}
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andEqualTo("isMarketable",searchMap.get("isMarketable"));
           	}
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andEqualTo("isEnableSpec", searchMap.get("isEnableSpec"));
           	}
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andEqualTo("isDelete",searchMap.get("isDelete"));
           	}
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andEqualTo("status",searchMap.get("status"));
           	}

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

    /**
     * 根据id查询商品
     * @param id
     * @return
     */
    @Override
    public Goods findGoodsById(String id) {
        //创建Goods对象，将查询到的spu，sku分别封装到对象中
        Goods goods = new Goods();
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        goods.setSpu(spu);

        //查询sku
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        //根据spu进行sku列表的查询
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        goods.setSkuList(skuList);

        return goods;
    }

    /**
     * 商品审核并自动上架
     * @param id
     */
    @Override
    @Transactional
    public void audit(String id) {
        //获取spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            //没有查询到spu
            throw new RuntimeException("当前商品不存在");
        }
        //判断商品(spu)是否处于删除状态
        if ("1".equals(spu.getIsDelete())){
            //处于删除状态，返回错误信息
            throw new RuntimeException("当前商品处于删除状态");
        }
        //未处于删除状态，审核并上架
        spu.setStatus("1");
        spu.setIsMarketable("1");
        //执行修改操作
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 商品下架
     * @param id
     */
    @Override
    @Transactional
    public void pull(String id) {
        //获取spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            //没有查询到spu
            throw new RuntimeException("当前商品不存在");
        }
        //判断商品(spu)是否处于删除状态
        if ("1".equals(spu.getIsDelete())){
            //处于删除状态，返回错误信息
            throw new RuntimeException("当前商品处于删除状态");
        }
        //未处于删除状态，下架
        spu.setIsMarketable("0");
        //执行修改操作
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 商品上架
     * @param id
     */
    @Override
    @Transactional
    public void put(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            throw new RuntimeException("当前商品不存在");
        }
        //商品审核状态必须为已审核(1)
        if (!spu.getStatus().equals("1")){
            throw new RuntimeException("当前商品未审核");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    @Transactional
    public void restore(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //判断当前商品必须处于已删除状态
        if (!"1".equals(spu.getIsDelete())){
            throw new RuntimeException("此商品未删除");
        }
        //修改相关的属性字段进行保存操作
        spu.setIsDelete("0");
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    @Override
    @Transactional
    public void realDel(String id) {
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //判断当前商品是否处于已删除状态
        if (!"1".equals(spu.getIsDelete())){
            throw new RuntimeException("当前商品处于未删除状态");
        }
        //执行删除操作
        spuMapper.deleteByPrimaryKey(id);
    }

}
