package za.co.infernos.mekanismelements.mixin;

import mekanism.generators.common.tile.TileEntityGasGenerator;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Reserved hook for Gas Burning Generator (no per-tick debug logging).
 * Left as an empty mixin target so older mixin configs that still list this class remain valid.
 */
@Mixin(value = TileEntityGasGenerator.class, remap = false)
public class MixinTileEntityGasGenerator {
}
