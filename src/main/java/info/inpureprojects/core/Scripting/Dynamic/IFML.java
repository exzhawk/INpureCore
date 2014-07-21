package info.inpureprojects.core.Scripting.Dynamic;

import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import info.inpureprojects.core.API.Events.EventRegisterTexture;

/**
 * Created by den on 7/19/2014.
 */
public interface IFML {

    @Subscribe
    public void onPreInit(FMLPreInitializationEvent evt);

    @Subscribe
    public void onInit(FMLInitializationEvent evt);

    @Subscribe
    public void onPostInit(FMLPostInitializationEvent evt);

    @Subscribe
    public void onBlockTextures(EventRegisterTexture.Block evt);

    @Subscribe
    public void onItemTextures(EventRegisterTexture.Item evt);
}
