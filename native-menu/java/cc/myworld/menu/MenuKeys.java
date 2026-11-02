package cc.myworld.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=RichliteMenu.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class MenuKeys {
    static final KeyMapping OPEN=new KeyMapping("key.richlite_menu.open",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_BACKSPACE,"key.categories.richlite_menu");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(OPEN);}

    @EventBusSubscriber(modid=RichliteMenu.ID,value=Dist.CLIENT)
    public static final class Tick {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event){
            Minecraft client=Minecraft.getInstance();
            while(OPEN.consumeClick())if(client.screen==null&&client.player!=null&&client.getConnection()!=null)client.getConnection().sendUnsignedCommand("menu");
        }
    }
}
