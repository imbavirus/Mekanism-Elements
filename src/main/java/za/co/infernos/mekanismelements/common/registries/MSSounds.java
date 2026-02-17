package za.co.infernos.mekanismelements.common.registries;

import za.co.infernos.mekanismelements.common.MekanismElements;
import mekanism.common.registration.impl.SoundEventDeferredRegister;
import mekanism.common.registration.impl.SoundEventRegistryObject;
import net.minecraft.sounds.SoundEvent;

public class MSSounds {
    public static final SoundEventDeferredRegister SOUND_EVENTS = new SoundEventDeferredRegister(MekanismElements.MODID);

    public static final SoundEventRegistryObject<SoundEvent> AIR_COMPRESSOR = SOUND_EVENTS.register("tile.machine.air_compressor");

    private MSSounds(){
    }
}

