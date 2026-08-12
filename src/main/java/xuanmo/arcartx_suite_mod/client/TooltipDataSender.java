package xuanmo.arcartx_suite_mod.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import priv.seventeen.artist.arcartx.common.api.ArcartXClient;

/**
 * 将采集到的 tooltip 数据通过 ArcartX 自定义网络包发送给服务端。
 * <p>
 * 使用 {@code ArcartXClient.sendPacket(packetId, data...)} 发送，
 * 服务端通过 {@code ClientPacketHandler} 接收。
 * <p>
 * 数据包格式：
 * <pre>
 * packetId: "AXS_TOOLTIP_DATA"
 * data[0]: 物品指纹（类型 ID + NBT hash，用于服务端去重）
 * data[1]: 物品类型 ID（如 "tacz:ak47" 或 "minecraft:diamond_sword"）
 * data[2]: tooltip 文本行（JSON 数组字符串，每行一条）
 * data[3]: 结构化数据（JSON 对象字符串，包含数值属性）
 * </pre>
 */
public final class TooltipDataSender {

    /** 数据包 ID，与服务端 ClientPacketHandler 约定 */
    public static final String PACKET_ID = "AXS_TOOLTIP_DATA";

    private TooltipDataSender() {}

    /**
     * 发送 tooltip 数据给服务端。
     *
     * @param stack 物品栈
     * @param textLines tooltip 文本行列表
     * @param structuredData 结构化数据 JSON 字符串
     */
    public static void send(ItemStack stack, List<String> textLines, String structuredData) {
        if (stack == null || stack.isEmpty()) return;
        if (textLines == null || textLines.isEmpty()) return;

        try {
            // 构建物品指纹：类型 ID + NBT hash
            String itemId = stack.getDescriptionId();
            int nbtHash = stack.hasTag() ? stack.getTag().hashCode() : 0;
            String fingerprint = itemId + "@" + nbtHash;

            // 构建文本行 JSON 数组
            StringBuilder linesJson = new StringBuilder("[");
            for (int i = 0; i < textLines.size(); i++) {
                if (i > 0) linesJson.append(",");
                linesJson.append("\"").append(escapeJson(textLines.get(i))).append("\"");
            }
            linesJson.append("]");

            // 构建数据包
            List<String> data = new ArrayList<>(4);
            data.add(fingerprint);
            data.add(itemId);
            data.add(linesJson.toString());
            data.add(structuredData != null ? structuredData : "{}");

            // 通过 ArcartX 网络包发送
            ArcartXClient.sendPacket(PACKET_ID, new ArrayList<>(data));
        } catch (Throwable t) {
            // 静默处理，避免影响游戏
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
