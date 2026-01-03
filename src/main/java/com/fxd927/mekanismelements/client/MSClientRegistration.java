package com.fxd927.mekanismelements.client;

import com.fxd927.mekanismelements.client.gui.machine.*;
import com.fxd927.mekanismelements.common.MekanismElements;
import com.fxd927.mekanismelements.common.registries.MSContainerTypes;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MekanismElements.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class MSClientRegistration {

    private MSClientRegistration() {
    }

    @SubscribeEvent
    public static void registerContainers(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, MSContainerTypes.ADSORPTION_SEPARATOR, GuiAdsorptionSeparator::new);
        ClientRegistrationUtil.registerScreen(event, MSContainerTypes.AIR_COMPRESSOR, GuiAirCompressor::new);
        //ClientRegistrationUtil.registerScreen(event, MSContainerTypes.CHEMICAL_DEMOLITION_MACHINE, GuiChemicalDemolitionMachine::new);
        ClientRegistrationUtil.registerScreen(event, MSContainerTypes.RADIATION_IRRADIATOR, GuiRadiationIrradiator::new);

        //ClientRegistrationUtil.registerScreen(event, MSContainerTypes.ADSORPTION_TYPE_SEAWATER_METAL_EXTRACTOR, GuiAdsorptionTypeSeawaterMetalExtractor::new);
        //ClientRegistrationUtil.registerScreen(event, MSContainerTypes.ORGANIC_LIQUID_EXTRACTOR, GuiOrganicLiquidExtractor::new);
        ClientRegistrationUtil.registerScreen(event, MSContainerTypes.SEAWATER_PUMP, GuiSeawaterPump::new);
    }
}
