# KPA BS3D Swing Addon

这个附属把 BS3D 的已穿戴容器统一接入玩家摆动，并额外注册一个可直接测试的“摆动容器”。每个容器在玩家身上同时跟随两套状态：

- BS3D 负责真实 `ItemStack`、容器容量、装备挂点、外观同步和 Blockbench 模型加载。
- Players-Animation 负责玩家多关节动画。世界渲染时，附属读取 KPA 当前帧的腰部或胸部骨骼矩阵，计算相对于原版姿态的增量，再交给 BS3D 的挂点变换。
- 摆动入口挂在 BS3D 的 `WorldEquipmentRenderer` 上，所有已登记且已穿戴的 BS3D 模型都经过同一套骨骼增量；腰部容器使用腰部骨骼，胸前／胸后容器使用胸部骨骼。
- 移动时再叠加一个受服务端参数控制的轻量惯性摆动；静止时不会凭空晃动。
- 附属自己的客户端与服务端通信只有 protobuf：客户端发送 `Hello`，服务端回传 `State`。物品注册也复用 BS3D 的 protobuf 清单，不复制 BS3D 库的库存或外观协议。

目标环境是 Minecraft 1.20.1、Fabric、Java 17。运行时必须安装对应版本的：

1. Inventory UI Framework（客户端与服务端产物）；
2. Backpack and Storage 3D（`backpack_storage_3d` 服务端 JAR 与 `backpack_storage_3d_client` 客户端 JAR）；
3. Kizuna Players Animation。

## 构建

默认从同级目录查找三个依赖源码目录，也可以显式指定：

```sh
./gradlew -PframeworkDir=/path/to/Kizuna-Inventory-UI-Framework \
  -PkpaDir=/path/to/Kizuna-Players-Animation \
  -Pbs3dDir=/path/to/backpack-and-storage-3D build
```

产物：

- `client/build/libs/kpa-bs3d-swing-addon-client-0.1.0-SNAPSHOT.jar`
- `server/build/libs/kpa-bs3d-swing-addon-server-0.1.0-SNAPSHOT.jar`
- `build/distributions/kpa-bs3d-swing-addon-0.1.0-SNAPSHOT-proto.zip`

客户端和专用服务器必须使用同一版本的物品清单。进入创造模式工具栏后可取得“摆动容器”，在 BS3D 工作台中拖到玩家腰部即可装备；F5 或附近玩家都能看到挂点和摆动。BS3D 自带草包、背篓及 Bong 的九种装备也沿用各自的腰部／胸部挂点进入同一摆动路径。

## 协议与边界

协议唯一来源是 [`container_swing.proto`](shared/src/main/proto/kizuna/kpabs3d/v1/container_swing.proto)。网络包使用 Fabric 通道 `kpa_bs3d_swing:hello` 和 `kpa_bs3d_swing:state`，不接受客户端提交摆幅、频率或阻尼。字段和错误边界见 [`docs/protocol.md`](docs/protocol.md)。

模型清单位于 [`items.txtpb`](shared/src/main/resources/bs3d/kpa_bs3d_swing/items.txtpb)，模型源为 [`SwingPouch.bbmodel`](shared/src/main/resources/bs3d/kpa_bs3d_swing/models/SwingPouch.bbmodel)。模型仍由 BS3D 的资源白名单加载，不在附属中另写渲染器或库存。

## 验证

已执行：

```sh
./gradlew :client:compileJava :server:compileJava
./gradlew :client:remapJar :server:remapJar
./gradlew :server:generateProto :client:generateProto protocolZip
```

另外已在 Windows Native / Minecraft 1.20.1 / Fabric 0.16.10 / Java 17 下启动整套依赖：Fabric 加载 73 个模组，BS3D 注册 9 个 Bong 容器，Addon 注册 1 个摆动容器；9 个 Bong 模型、草包、背篓和摆动容器模型均成功加载，protobuf 服务端／客户端入口初始化，未出现本 Addon 的 Mixin 错误。该启动记录证明所有模型进入了统一渲染装配；摆幅大小、不同体型和多人追踪仍应以游戏画面作最终视觉验收。

MIT License，版权归 Kizunad。
