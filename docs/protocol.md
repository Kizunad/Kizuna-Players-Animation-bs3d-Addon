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
