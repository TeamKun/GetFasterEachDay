package net.kunmc.lab.getfastereachday

import net.kunmc.lab.getfastereachday.flylib.SmartTabCompleter
import net.kunmc.lab.getfastereachday.flylib.TabChain
import net.kunmc.lab.getfastereachday.flylib.TabObject
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.w3c.dom.Attr
import kotlin.math.roundToLong

class GetFasterEachDay : JavaPlugin() , Listener{
    lateinit var manager: FasterManager
    lateinit var conf: ConfigManager
    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        conf = ConfigManager(this)
        manager = FasterManager(this)
        getCommand("gf")!!.setExecutor(GFCommand(this))
        getCommand("gf")!!.tabCompleter = GFCommand.gen()
        server.pluginManager.registerEvents(this,this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onPlayerLeave(e:PlayerQuitEvent){
        val list = e.player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.modifiers.filter { it.name === "GF" }
        list.forEach { e.player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.removeModifier(it) }
        FasterManager.EFFECTS.forEach { e.player.removePotionEffect(it) }
    }
}

class GFCommand(val plugin: GetFasterEachDay) : CommandExecutor {
    companion object {
        fun gen(): SmartTabCompleter {
            return SmartTabCompleter(
                TabChain(
                    TabObject(
                        "start"
                    )
                ),
                TabChain(
                    TabObject(
                        "end"
                    )
                ),
                TabChain(
                    TabObject(
                        "status"
                    )
                ),
                TabChain(
                    TabObject(
                        "change"
                    ),
                    TabObject(
                        "Range", "DayRate", "EffectRate", "SpeedRate", "EntityBoosted", "Rate"
                    )
                ),
                TabChain(
                    TabObject("pause")
                ),
                TabChain(
                    TabObject("skip")
                )
            )
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (sender.isOp) return run(sender, command, label, args)
        } else {
            return run(sender, command, label, args)
        }
        return false
    }

    fun run(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size == 1) {
            when (args[0]) {
                "s", "start" -> {
                    plugin.manager.start()
                    Bukkit.broadcastMessage("????????????!")
                }
                "e", "end" -> {
                    plugin.manager.end()
                    Bukkit.broadcastMessage("????????????!")
                }
                "status" -> {
                    sender.sendMessage(
                        "Now Effect Level:${
                            FasterManager.effectLevel(
                                plugin.manager.TickCount,
                                plugin.conf
                            )
                        }"
                    )
                    sender.sendMessage(
                        "Now Skip Day Tick:${
                            FasterManager.skipDayTick(
                                plugin.manager.TickCount,
                                plugin.conf
                            )
                        }"
                    )
                    sender.sendMessage(
                        "Now Power:${
                            FasterManager.getPower(
                                plugin.manager.TickCount,
                                plugin.conf
                            )
                        }"
                    )
                    if (sender is Player) {
                        sender.sendMessage(
                            "Now Attributes Size:${
                                (sender as Player).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.modifiers.size
                            }"
                        )

                        val list = sender.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.modifiers.map {
                            Pair(
                                it.name,
                                it.amount
                            )
                        }
                        list.forEachIndexed { index, pair ->
                            sender.sendMessage("Index:${index} Name:${pair.first} Amount:${pair.second}")
                        }
                    }
                }
                "pause" -> {
                    if(plugin.manager.isPaused){
                        plugin.manager.pause(false)
                        sender.sendMessage("???????????????!")
                    }else{
                        plugin.manager.pause(true)
                        sender.sendMessage("???????????????!")
                    }
                }
//                "c", "change" -> {
//                    plugin.manager.change()
//                    sender.sendMessage("Now Entity Boosted Value:${plugin.manager.EntityBoosted}")
//                }
                else -> {
                    return false
                }
            }
            return true
        } else if (args.size == 2) {
            when(args[0]){
                "skip" -> {
                    val float:Float? = args[1].toFloatOrNull()
                    if(float == null){
                        sender.sendMessage("???????????????????????????")
                        return false
                    }

                    plugin.manager.skip(float)
                    sender.sendMessage("${args[1]}????????????????????????")
                    return true
                }

                else -> return false
            }
        } else if (args.size == 3) {
            when (args[0]) {
                "change" -> {
                    when (args[1]) {
                        "Range" -> {
                            if (args[2].toDoubleOrNull() == null) return false
                            plugin.conf.Range = args[2].toDouble()
                            sender.sendMessage("Range???${args[2].toDouble()}?????????????????????")
                        }
                        "DayRate" -> {
                            if (args[2].toIntOrNull() == null) return false
                            plugin.conf.DayRate = args[2].toInt()
                            sender.sendMessage("DayRate???${args[2].toInt()}?????????????????????")
                        }
                        "EffectRate" -> {
                            if (args[2].toDoubleOrNull() == null) return false
                            plugin.conf.EffectRate = args[2].toDouble()
                            sender.sendMessage("EffectRate???${args[2].toDouble()}?????????????????????")
                        }
                        "SpeedRate" -> {
                            if (args[2].toDoubleOrNull() == null) return false
                            plugin.conf.SpeedRate = args[2].toDouble()
                            sender.sendMessage("SpeedRate???${args[2].toDouble()}?????????????????????")
                        }
                        "EntityBoosted" -> {
                            plugin.manager.EntityBoosted = args[2].toBoolean()
                            sender.sendMessage("EntityBoosted???${args[2].toBoolean()}?????????????????????")
                        }
                        "Rate" -> {
                            if (args[2].toDoubleOrNull() == null) return false
                            plugin.conf.SpeedRate = args[2].toDouble()
                            plugin.conf.DayRate = args[2].toInt()
                            sender.sendMessage("SpeedRate???${args[2].toDouble()}?????????????????????")
                            sender.sendMessage("DayRate???${args[2].toInt()}?????????????????????")
                        }
                        else -> {
                            return false
                        }
                    }
                    plugin.conf.save()
                    return true
                }
            }
        }
        return false
    }
}

class FasterManager(val plugin: GetFasterEachDay) : BukkitRunnable() {
    init {
        this.runTaskTimer(plugin, 1, 1)
    }

    var isGoingOn = false
        private set

    var TickCount: Long = 0
        private set

    var EntityBoosted = true

    var isPaused = false
    private set

    fun start() {
        isGoingOn = true
        TickCount = 0
        removeEffect()
    }

    fun end() {
        isGoingOn = false
        removeEffect()
    }

    fun pause(b:Boolean){
        isPaused = b
    }

    fun skip(f:Float){
        val v = f - 1 // ????????????
        TickCount = (v * 20.0 * plugin.conf.DayRate).roundToLong()
    }

    private fun removeEffect() {
        Bukkit.getOnlinePlayers().forEach { p ->

            val remove = mutableListOf<AttributeModifier>()
            p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.modifiers.filter { it.name === "GF" }
                .forEach { remove.add(it) }
            remove.forEach { p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.removeModifier(it) }
            p.sendMessage("Removed ${remove.size} Modifiers")
            EFFECTS.forEach { e ->
                p.removePotionEffect(e)
            }

            if (EntityBoosted) {
                p.getNearbyEntities(plugin.conf.Range, plugin.conf.Range, plugin.conf.Range)
                    .filter { it is LivingEntity }
                    .forEach { en ->
                        val rem = mutableListOf<AttributeModifier>()
                        (en as LivingEntity).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.modifiers.filter { it.name === "GF" }
                            .forEach { rem.add(it) }
                        rem.forEach {
                            (en as LivingEntity).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.removeModifier(it)
                        }

                        EFFECTS.forEach { e ->
                            (en as LivingEntity).removePotionEffect(e)
                        }
                    }
            }
        }
    }

    fun change() {
        EntityBoosted = !EntityBoosted
    }

    var lastEffectLevel = 0
    var lastSkipTick = 0.0
    var skipStack = 0.0

    override fun run() {
        if (isGoingOn) {
            if(!isPaused){
                TickCount += 1
            }


            val effectLevel = effectLevel(TickCount, plugin.conf)
            if (effectLevel > 0) {
                Bukkit.getOnlinePlayers().forEach { p ->
//                    addModifier(p)
                    EFFECTS.forEach {
                        p.addPotionEffect(PotionEffect(it, Int.MAX_VALUE, effectLevel))
                    }

                    if (EntityBoosted) {
                        p.getNearbyEntities(plugin.conf.Range, plugin.conf.Range, plugin.conf.Range)
                            .filter { it is LivingEntity }
                            .forEach { e ->
//                                EFFECTS.forEach {
//                                    (e as LivingEntity).addPotionEffect(PotionEffect(it, Int.MAX_VALUE, effectLevel))
//                                }
//                                addModifier(e as LivingEntity)
                            }
                    }

                }
            }
            if (effectLevel != lastEffectLevel) {
//                Bukkit.broadcastMessage("??????????????????????????????${effectLevel + 1}???????????????!")
                lastEffectLevel = effectLevel
            }

            Bukkit.getOnlinePlayers().forEach { p ->
                addModifier(p)
                if (EntityBoosted) {
                    p.getNearbyEntities(plugin.conf.Range, plugin.conf.Range, plugin.conf.Range)
                        .filter { it is LivingEntity }
                        .forEach { e ->
                            addModifier(e as LivingEntity)
                        }
                }
            }

            val skipTick = skipDayTick(TickCount, plugin.conf)
            skipStack += skipTick
            val skip = skipStack.toInt()
            if (skip >= 1) {
                skipStack -= skip
                Bukkit.getWorlds().forEach { it.time += skip }
                val displaySkippedTick = (skip * 10).toInt().toDouble() / 10
                if (displaySkippedTick != lastSkipTick) {
//                    Bukkit.broadcastMessage("??????${displaySkippedTick + 1}??????!")
                    lastSkipTick = displaySkippedTick
                }
            }

            val displaySkippedTick = (skipTick * 10).toInt().toDouble() / 10
            Bukkit.getOnlinePlayers().forEach { p ->
                p.sendActionBar("${displaySkippedTick + 1}??????")
            }
        }
    }


    fun addModifier(e: LivingEntity) {
        attachModifier(e, Attribute.GENERIC_MOVEMENT_SPEED, genModifier(TickCount, plugin.conf))
    }

    companion object {
        /**
         * ????????????1?????????????????????
         */
//        private const val EffectRate = 1
        fun effectLevel(ticks: Long, config: ConfigManager): Int {
            return (((ticks.toDouble() / 20.0) / 60.0) / config.EffectRate).toInt()
        }

        /**
         * ?????????????????????Tick??????1????????????
         */
//        private const val DayRate = 30
        fun skipDayTick(ticks: Long, config: ConfigManager): Double {
            return (ticks.toDouble() / 20.0) / config.DayRate
        }

        val EFFECTS = arrayListOf(PotionEffectType.FAST_DIGGING/*, PotionEffectType.SPEED*/)

        fun getPower(ticks: Long, config: ConfigManager): Double {
//            return ticks / config.PowerRate
            return (ticks.toDouble() / 20.0) / config.SpeedRate
        }

        fun genModifier(ticks: Long, config: ConfigManager): AttributeModifier {
            return AttributeModifier("GF", getPower(ticks, config), AttributeModifier.Operation.MULTIPLY_SCALAR_1)
        }

        fun attachModifier(entity: LivingEntity, attribute: Attribute, modifier: AttributeModifier) {
            val r = mutableListOf<AttributeModifier>()
            entity.getAttribute(attribute)!!.modifiers.filter { it.name === modifier.name }.forEach { r.add(it) }
            r.forEach { entity.getAttribute(attribute)!!.removeModifier(it) }
            entity.getAttribute(attribute)!!.addModifier(modifier)
        }
//        private val Range = 10.0
    }
}

class ConfigManager(val plugin: GetFasterEachDay) {
    var Range = plugin.config.getDouble("Range")
    var DayRate = plugin.config.getInt("DayRate")
    var EffectRate = plugin.config.getDouble("EffectRate")

    //    val PowerRate = plugin.config.getDouble("PowerRate")
    var SpeedRate = plugin.config.getDouble("SpeedRate")

    fun save() {
        plugin.config.set("Range", Range)
        plugin.config.set("DayRate", DayRate)
        plugin.config.set("EffectRate", EffectRate)
        plugin.config.set("SpeedRate", SpeedRate)
        plugin.saveConfig()
    }
}