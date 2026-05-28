# 修改菜品 — *Update Dish*

修改菜品和编辑员工是同一个套路：**回显接口**拉一份完整数据填到表单 → 用户改完调**更新接口**保存。区别是菜品还要同步更新 `dish_flavor` 表（先清空旧口味，再插新口味）。

*Updating a dish follows the same recipe as editing an employee: a **read-back endpoint** fetches the full data to prefill the form, then an **update endpoint** persists the user's changes. The twist is that dish updates also have to sync the `dish_flavor` table — wiping old flavors and inserting new ones.*

## 原型 — *Prototype*
![img_47.png](images/img_47.png)

## 接口 — *Endpoints*
![img_45.png](images/img_45.png)
![img_46.png](images/img_46.png)

---

## 1. 回显接口 — *Read-back Endpoint*

用户点"编辑"按钮时，前端先调这个接口拿到完整数据填进表单。

*When the user clicks "Edit", the frontend first calls this endpoint to fetch the full data and prefill the form.*

```text
GET /admin/dish/{id}
       ↓
DishController.getById(@PathVariable Long id)
       ↓
DishService.getByIdWithFlavor(id)
       ├── Dish dish = dishMapper.getById(id);                          / fetch the dish
       ├── List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id); / fetch its flavors
       └── DishVO vo = ... ; vo.setFlavors(flavors);                    / assemble VO
       ↓
Result.success(dishVO)
```

返回 `DishVO`（包含菜品基本字段 + 一个 `flavors` 列表），而不是裸 `Dish` 实体。

*The response is a `DishVO` (basic dish fields plus a `flavors` list), not the bare `Dish` entity.*

---

## 2. 更新接口 — *Update Endpoint*

用户改完表单后点"保存"。

*The user clicks "Save" after editing the form.*

```text
PUT /admin/dish  (body: DishDTO with flavors[])
       ↓
DishController.update(@RequestBody DishDTO dto)
       ↓
DishService.updateWithFlavor(dto)
       ├── ① 更新菜品主表 / update the main dish table
       ├── ② 删旧口味     / delete the old flavors
       └── ③ 插新口味     / insert the new flavors
       ↓
Result.success()
```

### 关键 Service 代码 — *Key Service Code*

```java
@Transactional   // 三步必须在一个事务里 / the three steps must be atomic
public void updateWithFlavor(DishDTO dishDTO) {
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO, dish);

    // ① 更新菜品主表（动态 SQL，只改非 null 字段） / update only non-null columns
    dishMapper.update(dish);

    // ② 删除旧口味 / wipe the existing flavors
    dishFlavorMapper.deleteByDishId(dishDTO.getId());

    // ③ 插入新口味（要回填 dishId） / insert new flavors after backfilling dishId
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if (flavors != null && !flavors.isEmpty()) {
        flavors.forEach(f -> f.setDishId(dishDTO.getId()));
        dishFlavorMapper.insertBatch(flavors);
    }
}
```

---

## 为什么是"先删后插"而不是"逐条更新"？ — *Why "Delete-Then-Insert" Instead of Per-Row Update?*

口味数量不固定 —— 用户可能减少、增加、或重新组合口味。逐条 diff 又繁又容易出 bug，**直接清空再插一份**才是最干净的策略，业内常用。

*The number of flavors is variable — a user may add, remove, or reorder them. Diffing one-by-one is tedious and error-prone; **clearing and re-inserting** is the simplest, most reliable strategy, and it's the industry-standard approach.*

---

## 几个细节 — *A Few Details*

- **同一个事务**：Service 方法加 `@Transactional`，三步要么全成功要么全回滚。

  ***Single transaction:** annotate the Service method with `@Transactional` so all three steps succeed or roll back together.*

- **口味的 `dish_id` 要回填**：从 DTO 里拿到的 flavor 对象本身没有 `dishId`，插入前要循环 `flavor.setDishId(dto.getId())`。

  ***Backfill `dish_id` on each flavor:** flavors arriving in the DTO have no `dishId`; before inserting, iterate and call `flavor.setDishId(dto.getId())`.*

- **动态 SQL `<set>`**：Mapper XML 里用 `<set>` 包住 `<if>`，只更新前端实际改动的字段。

  ***Dynamic SQL `<set>`:** in the Mapper XML, wrap `<if>` tags inside a `<set>` block so only the columns the user actually changed get updated.*

- **如果用了 Redis 缓存菜品列表，记得清缓存**：参考 `021 - day5 Redis 与 MySQL`，加 `@CacheEvict`。

  ***If dish lists are cached in Redis, evict the cache here:** see `021 - day5 Redis vs MySQL`, use `@CacheEvict`.*