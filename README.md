# 台球器材商城（微信小程序）

一个面向台球器材的**微信小程序 + Spring Boot 后台**一体化项目：小程序端提供商品浏览、比价、球杆推荐、皮头更换、社区发帖、下单与个人中心；后台提供商品 / 订单 / 帖子 / 用户 / 敏感词 / 数据统计等管理能力。

## 功能模块

**小程序端**

- **首页 / 分类**：按球杆、球桌、台呢、耗材配件等品类浏览商品
- **比价**：同款商品多店铺价格横向对比
- **球杆推荐**：根据预算、打法、手感偏好等条件推荐球杆
- **皮头更换 / 定制**：皮头改装与球杆定制下单
- **社区**：发帖、评论、点赞、收藏、我的帖子
- **订单**：下单、订单列表 / 详情、超时自动处理
- **个人中心**：登录、资料编辑、收货地址、收藏夹

**管理后台**（内置静态页，随后端一起启动）

- 商品 / 分类 / 品牌 / 球杆标签管理
- 订单管理、帖子审核、用户管理
- 敏感词管理、系统配置、数据统计、图片上传

## 技术栈

| 层 | 技术 |
|---|---|
| 小程序端 | 微信小程序原生框架（WXML / WXSS / JS） |
| 后端 | Spring Boot 2.7、MyBatis-Plus 3.5、MySQL 8、Redis、Hutool |
| 鉴权 | 拦截器 + Token（后台 BCrypt、小程序端 MD5 演示） |
| 后台页面 | 原生 HTML / CSS / jQuery 静态资源 |

## 目录结构

```
wechaminiPrograme
├── backend/                        # Spring Boot 后端
│   ├── src/main/java/com/billiard/
│   │   ├── controller/             # 小程序端接口
│   │   ├── controller/admin/       # 管理后台接口
│   │   ├── service/                # 业务逻辑（含球杆推荐规则引擎）
│   │   ├── mapper/                 # MyBatis-Plus Mapper
│   │   ├── entity/ dto/            # 实体与传输对象
│   │   ├── interceptor/            # 登录 / 管理员拦截器
│   │   └── task/                   # 订单超时定时任务
│   ├── src/main/resources/
│   │   ├── static/admin/           # 管理后台页面
│   │   ├── static/billiard_img/    # 商品图库（见下方说明，未随仓库提供）
│   │   ├── mapper/                 # 复杂查询 XML
│   │   └── application.yml         # 端口 / 数据库 / 图片地址配置
│   ├── uploads/                    # 运行时上传目录（头像、帖子图）
│   ├── billiard_db.sql             # 建库建表 + 演示数据
│   └── pom.xml
└── frontend/                       # 微信小程序
    ├── pages/                      # 20 个页面
    ├── utils/request.js            # 统一请求封装（BASE_URL 在此修改）
    ├── utils/image.js              # 图片地址补全与中文路径编码
    └── project.config.json         # 开发者工具项目配置
```

## 快速开始

### 1. 准备数据库

```bash
mysql -u root -p < backend/billiard_db.sql
```

脚本会**删除并重建** `billiard_db` 库（16 张表 + 演示数据），请确认无重要数据后再执行。

默认账号：

| 端 | 账号 | 密码 |
|---|---|---|
| 管理后台 | `admin` | `123456` |
| 小程序端 | `test` | `123456` |

### 2. 启动后端

环境要求：JDK 8+、Maven 3.6+、MySQL 8；Redis 可选（默认连 `127.0.0.1:6379`）。

```bash
cd backend
mvn spring-boot:run
```

启动后可访问：

- 管理后台：<http://127.0.0.1:8080/admin/>
- 图片资源：<http://127.0.0.1:8080/billiard_img/>

数据库账号默认 `root / 123456`，在 `backend/src/main/resources/application.yml` 中按需修改。

### 3. 运行小程序

1. 用**微信开发者工具**导入 `frontend` 目录
2. `project.config.json` 中的 `appid` 是原项目示例 AppID，请换成你自己的（无 AppID 可选「测试号」）
3. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」，否则无法访问 `http://127.0.0.1:8080`

## 服务地址配置（IP 已改为本机）

项目里所有服务地址都集中在 3 处，已统一改为 `127.0.0.1`，克隆后无需再改：

| 文件 | 说明 |
|---|---|
| `frontend/utils/request.js` | 小程序端 `BASE_URL` |
| `backend/src/main/resources/application.yml` | `image.base-url`（图片访问前缀） |
| `backend/src/main/resources/static/admin/js/common.js` | 管理后台 `BASE_URL` |

> 用**真机**调试时，手机上的 `127.0.0.1` 指向手机自己，需要把上述 3 处改成电脑的局域网 IP（如 `192.168.x.x`）。真机调试和模拟器表现不同，模拟器用 `127.0.0.1` 即可。

## 商品图库说明

`backend/src/main/resources/static/billiard_img/` 是约 360MB 的演示商品图片，**未包含在本仓库中**（体积原因）。缺少图片时商品会显示占位图，功能不受影响。如需完整效果，把原来那份 `billiard_img` 目录放回该路径即可。

`backend/uploads/` 是运行时上传目录（用户头像、帖子配图），已随仓库提供一份演示数据。

## 说明

- 本项目为课程 / 学习用途的演示项目，未做生产级安全加固（如 Token 过期策略、接口限流、图片防盗链等）。
- 数据库脚本中 `sys_config` 含演示用的商家邮寄地址与座机号，均为虚构数据。
