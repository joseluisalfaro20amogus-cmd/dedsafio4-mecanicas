package com.noxsafio.mecanicas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DEDSAFIO4 Mecánicas.
 * Comando: /mecanica activar <color> [segundos] | desactivar <color> | apagar_todas | lista | notificar <true|false>
 * Solo para operadores (nivel 2). Los jugadores en creativo/espectador no sufren las mecánicas.
 */
public class Mecanicas implements ModInitializer {

    /** Comando del mod DEDSAFIO4 original que muestra el aviso en pantalla. Cámbialo si tu sintaxis es otra. */
    private static final String COMANDO_NOTIFICAR = "notificar";

    private record Estado(int inicio, int fin) {} // fin = -1 -> sin límite

    private static final Map<Mecanica, Estado> ACTIVAS = new EnumMap<>(Mecanica.class);
    private static boolean usarNotificar = true;

    // Efectos al azar para el Momento Reviil (cámbialos a tu gusto)
    private static final List<RegistryEntry<StatusEffect>> REVIIL_EFECTOS = List.of(
            StatusEffects.SPEED, StatusEffects.JUMP_BOOST, StatusEffects.SLOW_FALLING,
            StatusEffects.GLOWING, StatusEffects.NIGHT_VISION, StatusEffects.STRENGTH,
            StatusEffects.REGENERATION, StatusEffects.BLINDNESS, StatusEffects.SLOWNESS,
            StatusEffects.HUNGER);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registrar(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(Mecanicas::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVAS.clear());

        // Ruleta verde: los zombis infectan al golpear
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (ACTIVAS.containsKey(Mecanica.VERDE)
                    && entity instanceof ServerPlayerEntity player
                    && !player.isCreative() && !player.isSpectator()
                    && source.getAttacker() instanceof ZombieEntity) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 600, 0));
            }
            return true;
        });
    }

    // ---------------------------------------------------------------- comandos

    private static void registrar(CommandDispatcher<ServerCommandSource> d) {
        SuggestionProvider<ServerCommandSource> sugerencias =
                (ctx, builder) -> CommandSource.suggestMatching(Mecanica.IDS, builder);

        d.register(CommandManager.literal("mecanica")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("activar")
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests(sugerencias)
                                .executes(c -> activar(c.getSource(), StringArgumentType.getString(c, "color"), -1))
                                .then(CommandManager.argument("segundos", IntegerArgumentType.integer(1, 86400))
                                        .executes(c -> activar(c.getSource(),
                                                StringArgumentType.getString(c, "color"),
                                                IntegerArgumentType.getInteger(c, "segundos"))))))
                .then(CommandManager.literal("desactivar")
                        .then(CommandManager.argument("color", StringArgumentType.word())
                                .suggests(sugerencias)
                                .executes(c -> desactivar(c.getSource(), StringArgumentType.getString(c, "color")))))
                .then(CommandManager.literal("apagar_todas")
                        .executes(c -> apagarTodas(c.getSource())))
                .then(CommandManager.literal("lista")
                        .executes(c -> lista(c.getSource())))
                .then(CommandManager.literal("notificar")
                        .then(CommandManager.argument("valor", BoolArgumentType.bool())
                                .executes(c -> {
                                    usarNotificar = BoolArgumentType.getBool(c, "valor");
                                    c.getSource().sendFeedback(() -> Text.literal(
                                            "Aviso con /" + COMANDO_NOTIFICAR + ": " + (usarNotificar ? "activado" : "desactivado")), true);
                                    return 1;
                                }))));
    }

    private static int activar(ServerCommandSource src, String id, int segundos) {
        Mecanica m = Mecanica.deId(id);
        if (m == null) {
            src.sendError(Text.literal("Ruleta desconocida: " + id + ". Opciones: " + String.join(", ", Mecanica.IDS)));
            return 0;
        }
        MinecraftServer server = src.getServer();
        int t = server.getTicks();
        int fin = segundos < 0 ? -1 : t + segundos * 20;
        ACTIVAS.put(m, new Estado(t, fin));
        avisar(server, m.tipoNotificar, m.mensaje);
        src.sendFeedback(() -> Text.literal("Mecánica " + m.id + " activada"
                + (segundos < 0 ? " (sin límite)" : " durante " + segundos + " s")), true);
        return 1;
    }

    private static int desactivar(ServerCommandSource src, String id) {
        Mecanica m = Mecanica.deId(id);
        if (m == null) {
            src.sendError(Text.literal("Ruleta desconocida: " + id));
            return 0;
        }
        if (ACTIVAS.remove(m) == null) {
            src.sendError(Text.literal("La mecánica " + m.id + " no estaba activa."));
            return 0;
        }
        avisar(src.getServer(), m.tipoNotificar, m.mensajeFin);
        src.sendFeedback(() -> Text.literal("Mecánica " + m.id + " desactivada"), true);
        return 1;
    }

    private static int apagarTodas(ServerCommandSource src) {
        int n = ACTIVAS.size();
        ACTIVAS.clear();
        src.sendFeedback(() -> Text.literal("Mecánicas apagadas: " + n), true);
        return n;
    }

    private static int lista(ServerCommandSource src) {
        if (ACTIVAS.isEmpty()) {
            src.sendFeedback(() -> Text.literal("No hay mecánicas activas."), false);
            return 0;
        }
        int t = src.getServer().getTicks();
        StringBuilder sb = new StringBuilder("Mecánicas activas: ");
        for (Map.Entry<Mecanica, Estado> e : ACTIVAS.entrySet()) {
            int fin = e.getValue().fin();
            sb.append(e.getKey().id).append(fin < 0 ? " (sin límite)" : " (" + Math.max(0, (fin - t) / 20) + " s)").append("  ");
        }
        String texto = sb.toString().trim();
        src.sendFeedback(() -> Text.literal(texto), false);
        return ACTIVAS.size();
    }

    /** Usa /notificar del mod DEDSAFIO4 si existe; si no, manda un mensaje normal al chat. */
    private static void avisar(MinecraftServer server, String tipo, String mensaje) {
        if (!usarNotificar) return;
        boolean existe = server.getCommandManager().getDispatcher().getRoot().getChild(COMANDO_NOTIFICAR) != null;
        if (existe) {
            try {
                server.getCommandManager().executeWithPrefix(
                        server.getCommandSource().withSilent(), COMANDO_NOTIFICAR + " " + tipo + " " + mensaje);
                return;
            } catch (Exception ignored) {
                // cae al mensaje normal
            }
        }
        server.getPlayerManager().broadcast(Text.literal(mensaje).formatted(Formatting.GOLD), false);
    }

    // ---------------------------------------------------------------- tick

    private static void tick(MinecraftServer server) {
        if (ACTIVAS.isEmpty()) return;
        int t = server.getTicks();

        // expirar las que tienen tiempo
        Iterator<Map.Entry<Mecanica, Estado>> it = ACTIVAS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mecanica, Estado> e = it.next();
            int fin = e.getValue().fin();
            if (fin >= 0 && t >= fin) {
                Mecanica m = e.getKey();
                it.remove();
                avisar(server, m.tipoNotificar, m.mensajeFin);
            }
        }
        if (ACTIVAS.isEmpty()) return;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            for (Map.Entry<Mecanica, Estado> e : ACTIVAS.entrySet()) {
                aplicar(e.getKey(), p, t - e.getValue().inicio());
            }
        }
    }

    /** r = ticks desde que se activó la mecánica (r == 0 justo al activarla). */
    private static void aplicar(Mecanica m, ServerPlayerEntity p, int r) {
        ServerWorld w = (ServerWorld) p.getWorld();
        switch (m) {
            case ROJA -> {
                if (r % 40 == 0) {
                    for (HostileEntity mob : w.getEntitiesByClass(HostileEntity.class,
                            p.getBoundingBox().expand(48.0), HostileEntity::isAlive)) {
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1));
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));
                    }
                }
            }
            case NARANJA -> {
                // Fuego eterno: solo el agua lo apaga (la resistencia al fuego protege)
                if (r % 20 == 0 && !p.isTouchingWater() && !p.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    p.setOnFireFor(3);
                }
            }
            case AMARILLA -> {
                // Radiación solar: de día, al cielo abierto, sin casco, en el Overworld
                if (r % 40 == 0 && w.getRegistryKey() == World.OVERWORLD
                        && w.isDay() && !w.isRaining()
                        && w.isSkyVisible(p.getBlockPos())
                        && p.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                    p.damage(w.getDamageSources().magic(), 1.0f);
                }
            }
            case VERDE -> { /* se gestiona en el evento ALLOW_DAMAGE */ }
            case AZUL -> {
                if (r % 20 == 0 && p.isTouchingWater()) {
                    p.damage(w.getDamageSources().magic(), 1.0f);
                }
            }
            case MORADA -> {
                if (r % 20 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 0, false, false));
                }
            }
            case CELESTE -> {
                if (r % 20 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0, false, false));
                }
                // El frío daña salvo cerca de una fuente de luz/calor
                if (r % 80 == 0 && !p.isOnFire() && w.getLightLevel(LightType.BLOCK, p.getBlockPos()) < 8) {
                    p.damage(w.getDamageSources().freeze(), 1.0f);
                }
            }
            case ROSA -> {
                if (r % 600 == 0) {
                    Random rnd = p.getRandom();
                    if (rnd.nextBoolean()) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1));
                        p.sendMessage(Text.literal("¡Regalo de las nutrias!").formatted(Formatting.LIGHT_PURPLE), true);
                    } else {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1));
                        p.sendMessage(Text.literal("¡Trampa! Las nutrias te la juegan...").formatted(Formatting.RED), true);
                    }
                }
            }
            case NUTRIA -> {
                if (r % 20 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 100, 0, false, false));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 100, 0, false, false));
                    if (p.isTouchingWater()) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, false, false));
                    }
                }
            }
            case REVIIL -> {
                if (r % 200 == 0) {
                    RegistryEntry<StatusEffect> efecto = REVIIL_EFECTOS.get(p.getRandom().nextInt(REVIIL_EFECTOS.size()));
                    int amp = (efecto == StatusEffects.SPEED || efecto == StatusEffects.STRENGTH || efecto == StatusEffects.JUMP_BOOST) ? 2 : 0;
                    p.addStatusEffect(new StatusEffectInstance(efecto, 200, amp));
                }
            }
        }
    }
}
