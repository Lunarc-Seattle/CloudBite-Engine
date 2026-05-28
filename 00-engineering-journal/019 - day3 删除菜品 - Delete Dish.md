# 删除菜品 — *Delete Dish*

## 原型 — *Prototype*
![img_48.png](images/img_48.png)

## 业务规则 — *Business Rules*

删除菜品的业务规则主要包括以下四条：

*The deletion rules for a dish consist of the following four constraints:*

1. **可以一次删除一个菜品，也可以批量删除菜品** —— 接口要同时支持单删和批删。

   ***Single delete and batch delete are both supported** — the endpoint accepts one or many dish IDs.*

2. **起售中（`status = 1`）的菜品不能删除** —— 必须先停售（status 改为 0）才能删，避免线上还在卖的菜品突然消失。

   ***A dish currently on sale (`status = 1`) cannot be deleted** — it must first be taken off-sale (status set to 0); otherwise customers might lose access to a dish mid-purchase.*

3. **被套餐关联的菜品不能删除** —— 否则套餐里会出现"幽灵菜品"。需要先在 `setmeal_dish` 表里查一下有没有引用。

   ***A dish referenced by a setmeal cannot be deleted** — otherwise the setmeal would point to a "ghost dish". The service must first query `setmeal_dish` to check for references.*

4. **删除菜品后，关联的口味数据也需要删除掉** —— `dish_flavor` 表里对应的行必须一起清掉，否则会留下孤儿数据。

   ***When a dish is deleted, its associated flavor rows must be deleted as well** — the matching records in `dish_flavor` have to be cleaned up too; otherwise they become orphan data.*

## 接口 — *Endpoint*
![img_49.png](images/img_49.png)

## 数据库关系 — *Database Relationships*
![img_50.png](images/img_50.png)

---

## 实现要点 — *Implementation Highlights*

整体调用路径：

*The end-to-end call path:*

```text
DELETE /admin/dish?ids=1,2,3
       ↓
DishController.delete(@RequestParam List<Long> ids)
       ↓
DishService.deleteBatch(ids)
       ├── ① 校验：起售中？被套餐引用？        / Validate: on sale? referenced by setmeal?
       ├── ② 删除 dish 表里的多条记录          / Batch-delete rows in dish
       └── ③ 删除 dish_flavor 里关联的口味     / Cascade-delete rows in dish_flavor
       ↓
Result.success()
```

### 关键 Service 代码 — *Key Service Code*

```java
@Transactional   // 4 步要么全成功要么全回滚 / all-or-nothing across the 4 steps
public void deleteBatch(List<Long> ids) {
    // ① 起售中不能删 / On-sale dishes cannot be deleted
    for (Long id : ids) {
        Dish dish = dishMapper.getById(id);
        if (dish.getStatus() == StatusConstant.ENABLE) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
    }

    // ② 被套餐关联的不能删 / Dishes referenced by a setmeal cannot be deleted
    List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
    if (setmealIds != null && !setmealIds.isEmpty()) {
        throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
    }

    // ③ 删菜品本体 / Delete the dishes themselves
    dishMapper.deleteByIds(ids);

    // ④ 删关联口味 / Delete the associated flavors
    dishFlavorMapper.deleteByDishIds(ids);
}
```

## 几个细节 — *A Few Details*

- **`@RequestParam List<Long> ids`**：前端用 `?ids=1,2,3` 传，Spring 自动按逗号拆成 `List`。

  ***`@RequestParam List<Long> ids`:** the frontend sends `?ids=1,2,3` and Spring auto-splits by comma into a `List`.*

- **校验顺序**：先校验"能不能删"，再真删 —— 避免删一半被异常打断造成脏数据。

  ***Validation order:** validate first, mutate second — otherwise a half-completed delete could be interrupted by an exception and leave the DB inconsistent.*

- **事务**：Service 方法加 `@Transactional`，删 `dish` 和删 `dish_flavor` 两步必须在同一事务里，要么都成要么都回滚。

  ***Transaction:** annotate the Service method with `@Transactional` so that deleting `dish` and deleting `dish_flavor` happen atomically.*

- **MyBatis 批量删除**：用 `<foreach>` 拼 `IN (?, ?, ?)`，**不要**循环单条执行 —— 那样 N 条数据要 N 次数据库往返。

  ***MyBatis batch delete:** use a `<foreach>` block to build `IN (?, ?, ?)` — do **not** loop and run one delete per ID; that would cost N round-trips to the database.*