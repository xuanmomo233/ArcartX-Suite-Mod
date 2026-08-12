package xuanmo.arcartx_suite_mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xuanmo.arcartx_suite_mod.client.TooltipEventListener;

/**
 * ArcartX Suite 客户端辅助 mod 主类。
 * <p>
 * 为 ArcartX 自定义 UI 提供 TACZ 枪械和 Apotheosis 神化词条的 tooltip 桥接。
 * 通过监听 {@code ItemTooltipEvent}，将动态生成的 tooltip 文本行注入到
 * ArcartX 的 tooltip 渲染流程中。
 */
@Mod(ArcartXSuiteMod.MOD_ID)
public class ArcartXSuiteMod {
    public static final String MOD_ID = "arcartx_suite_mod";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ArcartXSuiteMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 注册客户端 tooltip 事件监听器
        forgeBus.register(TooltipEventListener.class);

        LOGGER.info("ArcartX Suite Mod 已加载，tooltip 桥接已就绪。");
    }
}
