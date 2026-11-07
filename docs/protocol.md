# 摆动附属协议 v1

`shared/src/main/proto/kizuna/kpabs3d/v1/container_swing.proto` 是唯一协议源，Java 类型由 Gradle 的 `protoc` 任务生成，不提交生成文件。

客户端进入世界后向 `kpa_bs3d_swing:hello` 发送：

```text
Hello {
  protocol_version: 1
}
```

服务端校验版本和未知字段，再通过 `kpa_bs3d_swing:state` 回传：

| 字段 | 约束 | 作用 |
| --- | --- | --- |
| `protocol_version` | 必须为 `1` | 协议版本 |
| `enabled` | 服务端决定 | 是否启用摆动 |
| `amplitude_degrees` | `0..45` | 移动惯性摆幅 |
| `frequency_hz` | `(0,8]` | 摆动频率 |
| `damping` | `0..1` | 振幅衰减系数 |
| `revision` | 单调递增 | 防止旧状态覆盖新连接 |

网络载荷上限为 8 KiB。解析失败、未知字段、版本不匹配或数值越界时丢弃本次消息；客户端在收到合法服务端状态前保持禁用，不能自行改变服务端参数。

容器本身通过 BS3D 的 `bs3d:item_registries` protobuf 清单注册。摆动渲染不复制 BS3D 的库存和装备状态：模型身份、挂点和维度外观仍由 BS3D 的权威 `EquipmentAppearance` 提供，附属只在 `WorldEquipmentRenderer` 的既有挂点调用处叠加 KPA 当前帧的骨骼增量。

## 与即时搬运的关系

本附属只叠加身体骨骼和移动惯性的容器摆动，不订阅 MOVE/Transfer，不读取 duration_ticks 或 elapsed_ticks，也不播放搬运伸手动画。BS3D v7 去掉转移计时不会改变本附属协议或摆动参数；无需新增固定动画或等待服务端回执。
