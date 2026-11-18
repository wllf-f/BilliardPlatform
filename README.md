# 台球器材商城（微信小程序）

一个面向台球器材的 **微信小程序 + Spring Boot 后台** 一体化项目。

小程序端覆盖商品浏览、多店比价、球杆推荐、皮头更换、球杆定制、社区交流与下单流程；后台提供商品、订单、帖子、用户、敏感词与数据统计等管理能力。

## 功能特性

**小程序端（20 个页面）**

| 模块 | 说明 |
|---|---|
| 首页 / 分类 | 按国产小头杆、九球杆、球桌、台呢、耗材配件等品类浏览商品 |
| 商品列表 / 详情 | 关键词与分类筛选、规格与价格展示、收藏 |
| 比价 | 同款商品在不同店铺的价格横向对比 |
| 球杆推荐 | 根据预算、打法、手感偏好等条件，由后端规则引擎推荐球杆 |
| 皮头更换 / 球杆定制 | 单独的下单流程与商家邮寄地址展示 |
| 社区 | 发帖、评论、点赞、收藏、我的帖子，发帖内容经敏感词校验 |
| 订单 | 下单、订单列表与详情、超时自动处理、地址管理 |
| 我的 | 登录、资料编辑、收藏夹、收货地址 |

**管理后台**（静态页，随后端一起启动）

| 模块 | 说明 |
|---|---|
| 商品管理 | 商品、分类、品牌、价格、球杆标签的增删改查与图片上传 |
| 订单管理 | 订单查询、状态流转、发货与备注 |
| 社区管理 | 帖子、评论审核与管理 |
| 用户 / 管理员 | 用户列表、管理员账号与密码重置 |
| 系统设置 | 敏感词维护、系统参数（审核模式、邮寄地址等）配置 |
| 数据统计 | 商品、订单、用户、帖子等维度统计 |

## 技术栈

| 层 | 技术 |
|---|---|
| 小程序端 | 微信小程序原生框架（WXML / WXSS / JS） |
| 后端 | Spring Boot 2.7、MyBatis-Plus 3.5、MySQL 8、Redis、Hutool |
| 鉴权 | 拦截器 + Token（Redis 存储，滑动过期），密码 BCrypt 哈希（兼容旧 MD5 自动升级） |
| 后台页面 | 原生 HTML / CSS / jQuery 静态资源 |

## 项目结构

```
wechaminiPrograme
├── backend/                        # Spring Boot 后端
│   ├── src/main/java/com/billiard/
│   │   ├── controller/             # 小程序端接口
│   │   ├── controller/admin/       # 管理后台接口
│   │   ├── service/                # 业务逻辑（含球杆推荐规则引擎、敏感词过滤）
│   │   ├── mapper/                 # MyBatis-Plus Mapper
│   │   ├── entity/ dto/            # 实体与传输对象
│   │   ├── interceptor/            # 用户 / 管理员登录拦截器
│   │   ├── task/                   # 订单超时定时任务
│   │   └── util/                   # 密码、敏感词、用户上下文工具
│   ├── src/main/resources/
│   │   ├── static/admin/           # 管理后台页面
│   │   ├── static/billiard_img/    # 商品图库（未随仓库提供，见下文）
│   │   ├── mapper/                 # 复杂查询 XML
│   │   └── application.yml         # 端口 / 数据库 / Redis / 图片地址
│   ├── uploads/                    # 运行时上传目录（头像、帖子图、商品图）
│   ├── billiard_db.sql             # 建库建表 + 演示数据
│   └── pom.xml
└── frontend/                       # 微信小程序
    ├── pages/                      # 20 个页面
    ├── utils/request.js            # 统一请求封装（BASE_URL 在此修改）
    ├── utils/image.js              # 图片地址补全与中文路径编码
    └── project.config.json         # 开发者工具项目配置
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0
- Redis 6+（登录 Token 存在 Redis 中，不启动无法登录）
- 微信开发者工具

### 1. 初始化数据库

```bash
mysql -u root -p < backend/billiard_db.sql
```

脚本会**删除并重建** `billiard_db` 库（16 张表 + 演示数据），请确认无重要数据后再执行。

### 2. 启动 Redis

```bash
redis-server
```

默认连接 `127.0.0.1:6379`、`database 0`，可在 `application.yml` 中修改。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 服务地址：<http://127.0.0.1:8080>
- 管理后台：<http://127.0.0.1:8080/admin/>
- 图片资源：<http://127.0.0.1:8080/billiard_img/>

数据库账号默认 `root / 123456`，在 `backend/src/main/resources/application.yml` 中按需修改。

### 4. 运行小程序

1. 微信开发者工具导入 `frontend` 目录
2. `project.config.json` 中的 `appid` 为原项目示例 AppID，请换成自己的（或选择「测试号」）
3. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」，否则无法访问 `http://127.0.0.1:8080`

### 默认账号

| 端 | 账号 | 密码 |
|---|---|---|
| 管理后台 | `admin` | `123456` |
| 小程序端 | `test` | `123456` |

## 服务地址配置

项目里所有服务地址集中在 3 处，已统一为 `127.0.0.1`，克隆后无需再改：

| 文件 | 说明 |
|---|---|
| `frontend/utils/request.js` | 小程序端 `BASE_URL` |
| `backend/src/main/resources/application.yml` | `image.base-url`（图片访问前缀） |
| `backend/src/main/resources/static/admin/js/common.js` | 管理后台 `BASE_URL` |

> 用**真机**调试时，手机上的 `127.0.0.1` 指向手机自己，需要把上述 3 处改成电脑的局域网 IP（如 `192.168.x.x`），并保证手机与电脑在同一 WiFi 下。模拟器用 `127.0.0.1` 即可。

## 接口约定

- 请求头 `token: <登录返回的 token>`，由 `LoginInterceptor` 校验 Redis 中的会话并自动续期
- 管理后台接口统一走 `/admin/**`，由 `AdminInterceptor` 校验管理员身份
- 返回结构：`{ "code": 200, "msg": "success", "data": {} }`
- 未登录 / 登录过期返回 HTTP 401

## 商品图库说明

`backend/src/main/resources/static/billiard_img/` 是约 360MB 的演示商品图片，**未包含在本仓库中**（体积原因）。缺少图片时商品显示占位图，功能不受影响。如需完整效果，把原来那份 `billiard_img` 目录放回该路径即可（该目录已在 `.gitignore` 中忽略）。

`backend/uploads/` 是运行时上传目录（用户头像、帖子配图、商品图），已随仓库提供一份演示数据。

## 常见问题

| 现象 | 原因与解决 |
|---|---|
| 小程序请求全部失败 | 后端未启动，或未勾选「不校验合法域名」 |
| 真机无法访问 | `127.0.0.1` 需换成电脑局域网 IP |
| 登录提示 401 | Redis 未启动或已过期，重启 Redis 后重新登录 |
| 商品图片不显示 | 未放回 `billiard_img` 图库，见上一节 |

## 说明

- 本项目为课程 / 学习用途的演示项目，未做生产级安全加固（Token 过期策略、接口限流、图片防盗链等）。
- 数据库脚本中 `sys_config` 的商家邮寄地址与座机号、演示用户信息均为虚构数据。
