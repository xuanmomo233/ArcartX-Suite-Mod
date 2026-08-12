package xuanmo.arcartx_suite_mod.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xuanmo.arcartx_suite_mod.ArcartXSuiteMod;

/**
 * ItemTooltipEvent 监听器，将 TACZ 枪属性和 Apotheosis affix 的文本行
 * 注入到 {@code event.getToolTip()} 中，同时将数据发送给服务端。
 * <p>
 * 使用 LOW 优先级确保在 Apotheosis 的 HIGH 优先级监听器之后执行，
 * 这样 Apotheosis 已经插入了 affix 描述行，我们只需补充 TACZ 枪属性行。
 * <p>
 * 注入的文本行会被 ArcartX 的 {@code onTooltipRender} 从
 * {@code ClientTextTooltip} 组件中提取，显示在 ArcartX 自定义 UI 中。
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ArcartXSuiteMod.MOD_ID)
public class TooltipEventListener {

    private static final boolean DEBUG = Boolean.getBoolean("axs.tooltip.debug");

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Component> tooltip = event.getToolTip();

        // 1. TACZ 枪属性注入
        List<Component> taczLines = TaczTooltipCollector.collectTooltipLines(stack);
        if (!taczLines.isEmpty()) {
            int insertIndex = Math.min(1, tooltip.size());
            for (int i = taczLines.size() - 1; i >= 0; i--) {
                tooltip.add(insertIndex, taczLines.get(i));
            }
        }

        // 2. 采集完整 tooltip 文本行（包含 Apotheosis affix + TACZ 枪属性）
        //    发送给服务端，用于聊天预览 Lore 注入和业务逻辑
        sendTooltipDataToServer(stack, tooltip);
    }

    /**
     * LOWEST 优先级调试监听器：在所有其他 mod 的监听器执行完后，
     * 打印最终 tooltip 列表，包括每行的 Component 结构信息
     * （siblings、style、font 等），帮助判断哪些行可能被 ArcartX
     * 的 ClientTextTooltip 转换过滤掉。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltipDebug(ItemTooltipEvent event) {
        if (!DEBUG) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // 只记录 TACZ 枪械（避免日志爆炸）
        String descId = stack.getDescriptionId();
        if (!descId.contains("tacz")) return;

        List<Component> tooltip = event.getToolTip();
        System.out.println("[AXS-DEBUG] ItemTooltipEvent final tooltip for " + descId + " (" + tooltip.size() + " lines):");
        for (int i = 0; i < tooltip.size(); i++) {
            Component c = tooltip.get(i);
            Style style = c.getStyle();
            String styleInfo = "";
            if (style.getColor() != null) styleInfo += "color=" + style.getColor().getValue() + " ";
            if (style.isBold()) styleInfo += "B ";
            if (style.isItalic()) styleInfo += "I ";
            if (style.isUnderlined()) styleInfo += "U ";
            if (style.isStrikethrough()) styleInfo += "S ";
            if (style.isObfuscated()) styleInfo += "O ";
            if (style.getFont() != null) styleInfo += "font=" + style.getFont() + " ";
            // 检查是否有 insertion/clickEvent/hoverEvent
            if (style.getInsertion() != null) styleInfo += "insertion ";
            if (style.getClickEvent() != null) styleInfo += "clickEvent ";
            if (style.getHoverEvent() != null) styleInfo += "hoverEvent ";

            int siblings = c.getSiblings().size();
            String contentsType = c.getContents().getClass().getSimpleName();
            System.out.println("[AXS-DEBUG]   [" + i + "] \"" + c.getString() + "\" | type=" + contentsType
                + " | siblings=" + siblings
                + (styleInfo.isEmpty() ? "" : " | style: " + styleInfo.trim()));
        }
    }

    /**
     * 采集 tooltip 文本行和结构化数据，发送给服务端。
     * 客户端内置 2 秒冷却（同一物品指纹），避免每帧发包。
     */
    private static void sendTooltipDataToServer(ItemStack stack, List<Component> tooltip) {
        try {
            // 提取所有 tooltip 文本行（纯文本）
            List<String> textLines = new ArrayList<>(tooltip.size());
            for (Component component : tooltip) {
                String text = component.getString();
                if (text != null && !text.isBlank()) {
                    textLines.add(text);
                }
            }

            // 获取结构化数据（TACZ 枪属性）
            String structuredData = TaczTooltipCollector.collectStructuredData(stack);
            if (structuredData == null) {
                structuredData = "{}";
            }

            // 发送给服务端
            TooltipDataSender.send(stack, textLines, structuredData);
        } catch (Throwable t) {
            // 静默处理
        }
    }
}
