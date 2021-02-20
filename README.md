# simple-crud
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.loyayz/simple-crud/badge.svg)](https://mvnrepository.com/artifact/com.loyayz/simple-crud)
简单通用的单表增删改查。

## 1 本项目的主要目标
1. 开箱即用，无需任何配置，无需继承基类 Mapper 即可获得通用方法；
2. 简单直观，不使用插件，不使用拦截器，不修改 Mybatis 运行时的任何对象；
3. 全面贴心，不影响其他增强工具（如 Mybatis-Plus、通用 Mapper）；
4. 使用方便，提供 ActiveRecord 模式；

## 2 安装
```xml
<dependencies>
  <dependency>
    <groupId>com.loyayz</groupId>
    <artifactId>simple-crud</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```
## 3 快速设置
simple-crud 的基本原理是将扫描的实体类做为 BaseMapper<T> 的泛型动态注册到 Spring，并将实体类映射为数据库中的表和字段信息，因此无需继承基类 Mapper 即可使用通用方法。

### 3.1 定义基类/注解（可选）
```java
public abstract class MyBaseEntity {}

@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {}
```
基类和注解至少有一个，可自定义，也可直接使用默认的 com.loyayz.simple.BaseModel 或 JPA的javax.persistence.Table

### 3.2 设置扫描实体类
```java
import com.loyayz.simple.annotation.ModelScan;

@ModelScan(
  superClass = MyBaseEntity.class,
  annotationClass = Table.class
)
// 若用默认的 com.loyayz.simple.BaseModel 则只需要
// @ModelScan 即可
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

### 3.3 定义实体类
假设有一个表：
```sql
CREATE TABLE user
(
    id           VARCHAR(50) NOT NULL,
    name         VARCHAR(50) COMMENT '姓名',
    PRIMARY KEY (`id`)
);
```
对应的实体类
```java
@Table
public class User {
    String id;
    String name;
    //省略set和get方法
}
// 或
public class User extends MyBaseEntity {
    String id;
    String name;
    //省略set和get方法
}
```

### 3.4 使用
```java
@Autowire
BaseMapper<User> userMapper;

public void test() {
    User user = new User();
    user.setName("测试");
    userMapper.insert(user);
}
```

## 4 Active Record 模式
>**[Active Record](https://zh.wikipedia.org/wiki/Active_Record)** 模式
>
>在软件工程中，主动记录模式（active record pattern）是一种架构模式，可见于在关系数据库中存储内存中对象的软件中。
> 它在Martin Fowler的2003年著《企业应用架构的模式》书中命名。
> 符合这个模式的对象的接口将包括函数比如插入、更新和删除，加上直接对应于在底层数据库表格中列的或多或少的属性。
>
>主动记录模式是访问在数据库中的数据的一种方式。数据库表或视图被包装入类。
> 因此，对象实例被链接到这个表格的一个单一行。
> 在一个对象创建之后，在保存时将一个新行增加到表格中。
> 加载的任何对象都从数据库得到它的信息。
> 在一个对象被更新的时候，在表格中对应的行也被更新。
> 包装类为在表格或视图中的每个列都实现访问器方法或属性。

### 4.1 定义实体类
```java
import com.loyayz.simple.BaseModel;

// 实体类 implements BaseModel
public class User implements BaseModel {
//...
}
// 或基类 implements BaseModel
public abstract class MyBaseEntity implements BaseModel {}
public class User extends MyBaseEntity {
//...
}
```

### 4.2 使用
```java
    User user = new User();
    user.setName("测试");
    user.insert();
```

### 4.3 BaseModel 方法列表
`BaseModel` 接口中包含了实体类的基础 CRUD 方法，接口中全是默认方法，实体类继承就可以直接进行增删改查操作，下面是方法列表和介绍：

- `default boolean save()`：保存（id 为 null 则新增，id 不为 null 则修改）
- `default boolean insert()`：新增（非空字段）
- `default boolean batchInsert(List<T> entities)`：批量新增（所有字段）
- `default boolean deleteById(Serializable id)`：根据主键删除
- `default boolean deleteByIds(Collection<? extends Serializable> ids)`：根据主键批量删除
- `default boolean updateById()`：根据 id 修改（非空字段）
- `default boolean updateByIdWithNull()`：根据 id 修改（所有字段）
- `default T findById(Serializable id)`：根据 id 查询
- `default List<T> listByIds(List<? extends Serializable> ids)`：根据 ids 查询
- `default List<T> listByCondition(Sorter... sorters)`：根据非空字段查询列表
- `default Page<T> pageByCondition(int pageNum, int pageSize, Sorter... sorters)`：根据非空字段查询分页（基于 PageHelper，请自行添加依赖和配置）
- `default long countByCondition()`：根据非空字段查询总数
- `default long existByCondition()`：根据非空字段，查询是否存在记录
- `default <E extends Serializable> E idValue()`：主键值，建议子类替换为效率更高的判断方式（例如主键为 id 的情况下，直接 return id）
- `default BaseMapper<T> mapper()`：获取实体类做为泛型的 BaseMapper<T>
- `default ModelInfo modelInfo()`：获取实体类对应的信息

大部分方法都是以当前实体为参数进行增删改查的操作，少数几个方法可以通过传参进行其他的操作。
实际上除了以自己作为参数的方法外，其他外部参数的方法都应该设为静态方法才合适，由于当前接口的形式无法实现，只能通过创建一个实例后再进行操作。


## 5 其他
- 本项目所有依赖都为可选依赖，使用时请自行添加 Spring、Mybatis 依赖和配置
- 默认实体类映射规则：驼峰转下划线（类名->表名，属性名->字段名）
- 可通过 JPA 的 @Table 和 @Column 修改映射名（实现了 Table(name(),schema())、Column(name(),insertable(),updatable())）
- 关键字static、关键字transient、注解 javax.persistence.Transient 修饰的属性不映射为表字段
- 默认主键属性名为 id，可通过主键 javax.persistence.Id 声明属性为主键
- 默认主键策略为雪花算法，可通过 @IdStrategy 修改策略（可通过系统参数设置雪花算法全局起始时间，例 -Dsimple.snowflake=你的起始时间戳）


示例：
```java
import com.loyayz.simple.annotation.IdStrategy;
import com.loyayz.simple.annotation.IdStrategyType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "user")
public class SysUser {
    @Id
    @Column(name = "id")
    @IdStrategy(type = IdStrategyType.UUID)
    String userId;
    @Column(name = "name")
    String userName;
    //省略set和get方法
}
```

