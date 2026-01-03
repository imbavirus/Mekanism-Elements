# Compilation Errors Summary

## Current Status
**Total Real Errors (excluding "cannot find symbol" warnings): ~45** (Updated after fixing MobEffect issues)

## Recently Fixed
- ✅ MobEffect `applyEffectTick` → `tick()` method signature (3 files)
- ✅ MobEffectInstance constructor - using Holder<MobEffect> directly (2 files)
- ✅ DrugSyringe getEffectType() return type changed to Holder<MobEffect> (4 files)
- ✅ ResourceLocation constructor → ResourceLocation.fromNamespaceAndPath() (1 file)
- ✅ getUseDuration @Override removed (3 files)

---

## 1. JEI Recipe Category Issues (9 errors)

### BaseRecipeCategory Constructor (3 errors)
**Files:**
- `AdsorptionSeparatorRecipeCategory.java:39`
- `ChemicalDemolitionMachineRecipeCategory.java:34`
- `RadiationIrradiatorRecipeCategory.java:36`

**Error:** `no suitable constructor found for BaseRecipeCategory(IGuiHelper,IRecipeViewerRecipeType<...>,BlockRegistryObject<...>,int,int,int,int)`

**Issue:** The `BaseRecipeCategory` constructor signature has changed in the current Mekanism API. The current code is passing:
- `IGuiHelper helper`
- `IRecipeViewerRecipeType<RECIPE> recipeType`
- `BlockRegistryObject<...> block` (or `null` for ChemicalDemolitionMachine)
- `int xOffset, int yOffset, int width, int height`

**Needs:** Check Mekanism source code or documentation for the correct constructor signature.

---

### initChemical Method (6 errors)
**Files:**
- `AdsorptionSeparatorRecipeCategory.java:70`
- `ChemicalDemolitionMachineRecipeCategory.java:52`
- `RadiationIrradiatorRecipeCategory.java:55, 70`

**Error:** `method initChemical in class BaseRecipeCategory<RECIPE> cannot be applied to given types`

**Issue:** The `initChemical` method signature has changed. Current calls use:
```java
initChemical(builder, MekanismJEI.TYPE_CHEMICAL, RecipeIngredientRole.INPUT, gauge, scaledChemicals);
```

**Needs:** Verify the correct method signature for `initChemical` in `BaseRecipeCategory`.

---

## 2. JEI Recipe Type Registration (3 errors)

### SimpleRVRecipeType Constructor (2 errors)
**File:** `MSJEIRecipeType.java:11, 21`

**Error:** `cannot infer type arguments for SimpleRVRecipeType<>`

**Issue:** The `SimpleRVRecipeType` constructor parameters don't match the expected signature. Current code:
```java
new SimpleRVRecipeType<>(
    MSRecipeType.ADSORPTION.getRegistryName(),
    MekanismElements.rl("textures/gui/jei/adsorption_separator.png"),
    MSBlocks.ADSORPTION_SEPARATOR,
    AdsorptionRecipe.class,
    MSRecipeType.ADSORPTION,
    3, 3, 170, 79,
    MSBlocks.ADSORPTION_SEPARATOR
)
```

**Needs:** Check Mekanism source for correct `SimpleRVRecipeType` constructor signature.

---

### MSRecipeRegistryHelper Type Bounds (1 error)
**File:** `MSRecipeRegistryHelper.java:18`

**Error:** `type argument RECIPE#1 is not within bounds of type-variable RECIPE#2`

**Issue:** Generic type bounds mismatch between `IRecipeViewerRecipeType<RECIPE>` and `IMSRecipeTypeProvider<RECIPE, ?>`.

**Current code:**
```java
public static <RECIPE extends MekanismRecipe> void register(
    IRecipeRegistration registry, 
    IRecipeViewerRecipeType<RECIPE> recipeType,
    IMSRecipeTypeProvider<RECIPE, ?> type
)
```

**Needs:** Align generic type bounds or adjust the method signature.

---

## 3. Client Registration (8 errors)

### ClientRegistrationUtil.registerScreen (8 errors)
**File:** `MSClientRegistration.java:23, 24, 26, 30` (2 errors each)

**Error:** 
- `method registerScreen in class ClientRegistrationUtil cannot be applied to given types`
- `incompatible types: cannot infer type-variable(s) C,U`

**Issue:** The `registerScreen` method signature has changed. Current calls:
```java
ClientRegistrationUtil.registerScreen(MSContainerTypes.ADSORPTION_SEPARATOR, GuiAdsorptionSeparator::new);
```

**Needs:** Check Mekanism source for correct `ClientRegistrationUtil.registerScreen` signature. May need to pass additional parameters or use a different registration method.

---

## 4. Config System (13 errors)

### CachedLongValue.define (10 errors)
**Files:**
- `MSStorageConfig.java:21, 23, 25, 27, 29` (5 errors)
- `MSUsageConfig.java:22, 23, 24, 25, 26` (5 errors)

**Error:** `method define in class CachedLongValue cannot be applied to given types`

**Current code:**
```java
airCompressor = CachedLongValue.define(this, builder, "Base energy storage (Joules).", "airCompressor", 40_000L);
```

**Needs:** Check Mekanism source for correct `CachedLongValue.define` signature. Parameters may have changed order or types.

---

### getTranslation() Method (2 errors)
**Files:**
- `MSStorageConfig.java:8`
- `MSUsageConfig.java:8`

**Error:** `MSStorageConfig is not abstract and does not override abstract method getTranslation() in IMekanismConfig`

**Issue:** `BaseMekanismConfig` now requires implementing `getTranslation()` method.

**Needs:** Add `getTranslation()` method implementation to both config classes.

---

### addToContainer() Override (1 error)
**Files:**
- `MSStorageConfig.java:50`
- `MSUsageConfig.java:47`

**Error:** `method does not override or implement a method from a supertype`

**Issue:** `addToContainer()` method signature may have changed or been removed.

**Needs:** Check if `addToContainer()` is still part of `BaseMekanismConfig` or if it should be removed.

---

### MekanismConfigHelper.registerConfig (2 errors)
**File:** `MSConfig.java:16, 17`

**Error:** `method registerConfig in class MekanismConfigHelper cannot be applied to given types`

**Current code:**
```java
MekanismConfigHelper.registerConfig(modContainer, storageConfig);
```

**Needs:** Check Mekanism source for correct `MekanismConfigHelper.registerConfig` signature.

---

## 5. MobEffect API Changes (12 errors)

### applyEffectTick Signature (6 errors)
**Files:**
- `GoodSleep.java:14`
- `RadiationResistance.java:14`
- `SensoryParalysis.java:16`

**Error:** `applyEffectTick(LivingEntity,int) in [Class] cannot override applyEffectTick(LivingEntity,int) in MobEffect`

**Issue:** The `applyEffectTick` method signature has changed in Minecraft 1.21.1. It may now require different parameters or return type.

**Needs:** Check Minecraft/Mekanism API for correct `applyEffectTick` signature. May need to use `tick()` or a different method.

---

### @Override Annotations (4 errors)
**Files:**
- `GoodSleep.java:13, 20`
- `RadiationResistance.java:13, 20`
- `SensoryParalysis.java:15, 22, 30`

**Error:** `method does not override or implement a method from a supertype`

**Issue:** Methods marked with `@Override` no longer override superclass methods due to API changes.

**Needs:** Remove incorrect `@Override` annotations or update method signatures to match new API.

---

### removeAttributeModifiers (1 error)
**File:** `SensoryParalysis.java:24`

**Error:** `method removeAttributeModifiers in class MobEffect cannot be applied to given types`

**Issue:** `removeAttributeModifiers` method signature has changed.

**Needs:** Check correct signature for `removeAttributeModifiers` in new MobEffect API.

---

### MobEffectInstance Constructor (1 error)
**File:** `IodineTablet.java:26, 36`

**Error:** `incompatible types: MobEffect cannot be converted to Holder<MobEffect>`

**Current code:**
```java
player.getEffect(MSEffects.RADIATION_RESISTANCE.get());
player.addEffect(new MobEffectInstance(MSEffects.RADIATION_RESISTANCE.get(), newDuration, 0));
```

**Issue:** `MobEffectInstance` constructor now requires `Holder<MobEffect>` instead of `MobEffect`.

**Needs:** Change `MSEffects.RADIATION_RESISTANCE.get()` to use `MSEffects.RADIATION_RESISTANCE` directly (the Holder) or wrap it properly.

---

## 6. Type Inference Issues (2 errors)

### ChemicalUtil.emit (1 error)
**File:** `TileEntityAirCompressor.java:151`

**Error:** `incompatible types: inference variable T has incompatible bounds`

**Issue:** Type inference failing for `ChemicalUtil.emit` call.

**Current code:**
```java
@SuppressWarnings({"unchecked", "rawtypes"})
Object tankObj = chemicalTank;
ChemicalUtil.emit(Collections.singleton(Direction.UP), (IChemicalTank) tankObj, this, 256L * (1 + upgradeComponent.getUpgrades(Upgrade.SPEED)));
```

**Needs:** May need explicit type parameters or different casting approach.

---

### DrugSyringe MobEffect (1 error)
**File:** `DrugSyringe.java:77`

**Error:** `incompatible types: MobEffect cannot be converted to Holder<MobEffect>`

**Issue:** Same as IodineTablet - MobEffectInstance constructor requires Holder<MobEffect>.

**Needs:** Update to use Holder<MobEffect> instead of MobEffect.

---

## Recommended Fix Order

1. **MobEffect API Changes** (12 errors) - Most straightforward, just API signature updates
2. **Config System** (13 errors) - Check Mekanism config examples
3. **JEI Recipe Categories** (9 errors) - May need to check Mekanism source code
4. **Client Registration** (8 errors) - Check Mekanism client registration examples
5. **JEI Recipe Type Registration** (3 errors) - Check Mekanism JEI integration examples
6. **Type Inference** (2 errors) - May resolve after other fixes

---

## Resources Needed

1. **Mekanism Source Code** - To check correct method signatures
2. **Mekanism Examples** - Other mods using Mekanism API
3. **Minecraft 1.21.1 API Documentation** - For MobEffect changes
4. **NeoForge Documentation** - For registration changes

