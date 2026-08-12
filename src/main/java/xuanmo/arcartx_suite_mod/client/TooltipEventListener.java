package xuanmo.arcartx_suite_mod.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
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
     * 打印最终 tooltip 列表，确认 Apotheosis 的行是否在其中。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltipDebug(ItemTooltipEvent event) {
        if (!DEBUG) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        List<Component> tooltip = event.getToolTip();
        System.out.println("[AXS-DEBUG] ItemTooltipEvent final tooltip for " + stack.getDescriptionId() + " (" + tooltip.size() + " lines):");
        for (int i = 0; i < tooltip.size(); i++) {
            Component c = tooltip.get(i);
            System.out.println("[AXS-DEBUG]   [" + i + "] " + c.getString() + " | type=" + c.getContents().getClass().getSimpleName());
        }
    }

    /**
     * 调试：GatherComponents 事件在 Minecraft 将 Component 列表转换为
     * ClientTooltipComponent 列表之后、实际渲染之前触发。
     * 这里可以看到最终的 Either 列表，确认哪些行是文本、哪些是图片。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGatherComponentsDebug(RenderTooltipEvent.GatherComponents event) {
        if (!DEBUG) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // 只记录有 affix 标记或 TACZ 的物品
        String descId = stack.getDescriptionId();
        if (!descId.contains("tacz") && !descId.contains("apotheosis")) return;

        List<Either<TooltipComponent, Component>> elements = event.getTooltipElements();
        System.out.println("[AXS-DEBUG] GatherComponents for " + descId + " (" + elements.size() + " elements):");
        for (int i = 0; i < elements.size(); i++) {
            Either<TooltipComponent, Component> e = elements.get(i);
            String info = e.map(
                tc -> "IMAGE/TOOLTIP_COMPONENT type=" + tc.getClass().getSimpleName(),
                comp -> "TEXT: " + comp.getString() + " | type=" + comp.getContents().getClass().getSimpleName()
            );
            System.out.println("[AXS-DEBUG]   [" + i + "] " + info);
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
