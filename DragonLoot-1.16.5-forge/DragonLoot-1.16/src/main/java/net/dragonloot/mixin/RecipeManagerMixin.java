package net.dragonloot.mixin;

import com.google.gson.JsonElement;

import net.dragonloot.compat.recipes.RecipeGenerator;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(method = "apply", at = @At("HEAD"))
    private void applyMixin(Map<ResourceLocation, JsonElement> map, IResourceManager resourceManager, IProfiler profiler, CallbackInfo info) {
        for (ResourceLocation id : RecipeGenerator.RECIPES.keySet()) {
            map.put(id, RecipeGenerator.RECIPES.get(id));
        }
    }

}
