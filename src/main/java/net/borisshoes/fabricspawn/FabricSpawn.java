package net.borisshoes.fabricspawn;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.borisshoes.fabricspawn.utils.ConfigUtils;
import net.borisshoes.fabricspawn.utils.TeleportUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

// Large quantities of code taken from github.com/CodedSakura/FabricTPA
public class FabricSpawn implements ModInitializer {
   private static final Logger logger = LogManager.getLogger("FabricSpawn");
   private static final String CONFIG_NAME = "FabricSpawn.properties";
   
   private final HashMap<UUID, Long> recentRequests = new HashMap<>();
   private ConfigUtils config;
   
   @Override
   public void onInitialize(){
       logger.info("Initializing FabricSpawn...");
    
       config = new ConfigUtils(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_NAME).toFile(), logger, Arrays.asList(new ConfigUtils.IConfigValue[] {
             new ConfigUtils.IntegerConfigValue("stand-still", 5, new ConfigUtils.IntegerConfigValue.IntLimits(0),
                   new ConfigUtils.Command("Stand-Still time is %s seconds", "Stand-Still time set to %s seconds")),
             new ConfigUtils.IntegerConfigValue("cooldown", 5, new ConfigUtils.IntegerConfigValue.IntLimits(0),
                   new ConfigUtils.Command("Cooldown is %s seconds", "Cooldown set to %s seconds")),
             new ConfigUtils.BooleanConfigValue("bossbar", true,
                   new ConfigUtils.Command("Boss-Bar on: %s", "Boss-Bar is now: %s"))
       }));
   
   
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, registrationEnvironment) -> {
         dispatcher.register(CommandManager.literal("spawn")
               .executes(ctx -> spawntp(ctx)));
         
         dispatcher.register(config.generateCommand("spawntpconfig"));
      });
   }
   
   private int spawntp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException{
      ServerPlayerEntity player = ctx.getSource().getPlayer();
      if(checkCooldown(player)) return 1;
      ServerWorld world = player.getServer().getWorld(ServerWorld.OVERWORLD);
      TeleportUtils.genericTeleport((boolean) config.getValue("bossbar"), (int) config.getValue("stand-still"), player, () -> {
         player.teleport(world, world.getSpawnPos().getX(),world.getSpawnPos().getY(),world.getSpawnPos().getZ(),world.getSpawnAngle(),0.0f);
         recentRequests.put(player.getUuid(),Instant.now().getEpochSecond());
      });
      return 1;
   }
   
   
   private boolean checkCooldown(ServerPlayerEntity tFrom) {
      if (recentRequests.containsKey(tFrom.getUuid())) {
         long diff = Instant.now().getEpochSecond() - recentRequests.get(tFrom.getUuid());
         if (diff < (int) config.getValue("cooldown")) {
            tFrom.sendMessage(MutableText.of(new LiteralTextContent("You cannot make a request for ")).append(String.valueOf((int) config.getValue("cooldown") - diff))
                  .append(" more seconds!").formatted(Formatting.RED), false);
            return true;
         }
      }
      return false;
   }
   
}
