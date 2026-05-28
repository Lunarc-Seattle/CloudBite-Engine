package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServicempl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品和对应的口味
     */
    @Transactional
    //表示要么全成功要么全失败 因为要操作多张表
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish= new Dish();
        //不用传DTO，只需要自己new一个dish对象就好
        //然后把属性拷贝进去
        BeanUtils.copyProperties(dishDTO,dish);

        //像菜品表插入1条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
        Long dishid = dish.getId();


        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() >0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishid);
            });
            //遍历并且把上面获得的dishid 赋值
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 先判断菜品能不能删除 --  是否存在起售的菜品？？

        for ( Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断当前菜品是否能被删除 -- 是否被套餐关联了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIDsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() != 0){
            //当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 删除菜品表中的菜品数据
        for(Long id:ids) {
            dishMapper.deleteById();
            //删除菜品表中的菜品数据
            //也不管是不是有关联（还是其他）直接根据 id去删
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品和口味数据
     * @param id
     * @return
     */

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //两张表  最后结果都封装进 DishVO

        //1。根据id 查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 2 。根据菜品id查询口味数据
        List <DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //结果都封装进 DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
     // 先全部删除，再吧list里的全部插入

     //1 修改菜品的基本信息
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO,dish);
    dishMapper.update(dish);

     // 2 删除原有的口味数据
    dishFlavorMapper.deleteByDishId(dishDTO.getId());

     // 3 重新插入口味数据
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if(flavors != null && flavors.size() >0){
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dishDTO.getId());
        });
        //遍历并且把上面获得的dishid 赋值
        //向口味表插入n条数据
        dishFlavorMapper.insertBatch(flavors);
        }
    }
}
