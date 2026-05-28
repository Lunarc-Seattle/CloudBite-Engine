# 编辑员工 — *Edit Employee*

> "编辑"功能本质上是**两个接口配合**：一个回显（点击"编辑"按钮时把原始数据查出来填进表单），一个保存（用户改完点"保存"提交）。
>
> *The "edit" feature is really **two endpoints working together**: one for read-back (when the user clicks "Edit", the original data is fetched and prefilled into the form), and one for save (when the user submits the modified form).*

![img_27.png](images/img_27.png)

## 一个是获取 — *One Endpoint Is for Fetching (Get)*

```text
GET /admin/employee/{id}
       ↓
EmployeeController.getById(@PathVariable Long id)
       ↓
EmployeeService.getById(id)
       ↓
employeeMapper.getById(id)   ← SELECT * FROM employee WHERE id = ?
       ↓
Result.success(employee)
```

*`GET /admin/employee/{id}` → `EmployeeController.getById(id)` → `EmployeeService.getById(id)` → `employeeMapper.getById(id)` (issues `SELECT * FROM employee WHERE id = ?`) → returns the entity wrapped in `Result.success(employee)`.*

![img_29.png](images/img_29.png)

## 一个是编辑 — *The Other Is for Editing (Update)*

```text
PUT /admin/employee  (body: EmployeeDTO)
       ↓
EmployeeController.update(@RequestBody EmployeeDTO dto)
       ↓
EmployeeService.update(dto)
       ├── BeanUtils.copyProperties(dto, employee)
       ├── employee.setUpdateTime(LocalDateTime.now())          ← 实际项目用 AOP 自动填充 / handled by AOP in real projects
       └── employee.setUpdateUser(BaseContext.getCurrentId())   ← 同上 / same
       ↓
employeeMapper.update(employee)   ← 动态 SQL：只更新非 null 字段
       ↓
Result.success()
```

*`PUT /admin/employee` with `EmployeeDTO` in the body → `EmployeeController.update(dto)` → `EmployeeService.update(dto)` → copy properties from DTO to entity → stamp `updateTime` / `updateUser` (later automated via AOP, see file 010) → `employeeMapper.update(employee)` using a dynamic SQL that only updates non-null fields → `Result.success()`.*

## 几个细节 — *A Few Details*

- **回显用 GET 而不是 POST**：语义上"查一个资源"，REST 风格 / *Use `GET` for read-back, not `POST` — semantically "fetch one resource", proper REST style.*
- **保存用 PUT 而不是 POST**：语义上"替换/修改资源"，新增才用 POST / *Use `PUT` for save (modify), not `POST` — `POST` is reserved for create.*
- **`@PathVariable` vs `@RequestBody`**：路径上的 `{id}` 用前者，请求体里的 JSON 用后者 / *`@PathVariable` for `{id}` in the URL; `@RequestBody` for JSON in the request body.*
- **`<set>` 动态 SQL**：Mapper XML 里用 `<set>` 标签包住 `<if>`，只更新前端实际改动的字段 / *Use a `<set>` block wrapping `<if>` tags in the Mapper XML so only fields the user actually changed get updated.*
