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
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.w3c.dom.Attr

class GetFasterEachDay : JavaPlugin() {
    lateinit var manager: FasterManager
    lateinit var conf: ConfigManager
    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        conf = ConfigManager(this)
        manager = FasterManager(this)
        getCommand("gf")!!.setExecutor(GFCommand(this))
        getCommand("gf")!!.tabCompleter = GFCommand.gen()
    }

    override fun onDisable() {
        // Plugin shutdown logic
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
                        "Range", "DayRate", "EffectRate", "SpeedRate", "EntityBoosted","Rate"
                    )
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
                    Bukkit.broadcastMessage("加速開始!")
                }
                "e", "end" -> {
                    plugin.manager.end()
                    Bukkit.broadcastMessage("加速終了!")
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
//                "c", "change" -> {
//                    plugin.manager.change()
//                    sender.sendMessage("Now Entity Boosted Value:${plugin.manager.EntityBoosted}")
//                }
                else -> {
                    return false
                }
            }
            return true
        } else if (args.size == 3) {
            when (args[0]) {
                "change" -> {
                    when (args[1]) {
                        "Range" -> {
                            if(args[2].toDoubleOrNull() == null) return false
                            plugin.conf.Range = args[2].toDouble()
                            sender.sendMessage("Rangeを${args[2].toDouble()}に変更しました")
                        }
                        "DayRate" -> {
                            if(args[2].toIntOrNull() == null) return false
                            plugin.conf.DayRate = args[2].toInt()
                            sender.sendMessage("DayRateを${args[2].toInt()}に変更しました")
                        }
                        "EffectRate" -> {
                            if(args[2].toDoubleOrNull() == null) return false
                            plugin.conf.EffectRate = args[2].toDouble()
                            sender.sendMessage("EffectRateを${args[2].toDouble()}に変更しました")
                        }
                        "SpeedRate" -> {
                            if(args[2].toDoubleOrNull() == null) return false
                            plugin.conf.SpeedRate = args[2].toDouble()
                            sender.sendMessage("SpeedRateを${args[2].toDouble()}に変更しました")
                        }
                        "EntityBoosted" -> {
                            plugin.manager.EntityBoosted = args[2].toBoolean()
                            sender.sendMessage("EntityBoostedを${args[2].toBoolean()}に変更しました")
                        }
                        "Rate" -> {
                            if(args[2].toDoubleOrNull() == null) return false
                            plugin.conf.SpeedRate = args[2].toDouble()
                            plugin.conf.DayRate = args[2].toInt()
                            sender.sendMessage("SpeedRateを${args[2].toDouble()}に変更しました")
                            sender.sendMessage("DayRateを${args[2].toInt()}に変更しました")
                        }
                        else -> {return false}
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

    fun start() {
        isGoingOn = true
        TickCount = 0
        removeEffect()
    }

    fun end() {
        isGoingOn = false
        removeEffect()
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
            TickCount += 1


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
//                Bukkit.broadcastMessage("エフェクトのレベルが${effectLevel + 1}に上がった!")
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
//                    Bukkit.broadcastMessage("現在${displaySkippedTick + 1}倍速!")
                    lastSkipTick = displaySkippedTick
                }
            }

            val displaySkippedTick = (skipTick * 10).toInt().toDouble() / 10
            Bukkit.getOnlinePlayers().forEach { p ->
                p.sendActionBar("${displaySkippedTick + 1}倍速 採掘速度レベル:${lastEffectLevel + 1}")
            }
        }
    }


    fun addModifier(e: LivingEntity) {
        attachModifier(e, Attribute.GENERIC_MOVEMENT_SPEED, genModifier(TickCount, plugin.conf))
    }

    companion object {
        /**
         * 何分間に1レベル上がるか
         */
//        private const val EffectRate = 1
        fun effectLevel(ticks: Long, config: ConfigManager): Int {
            return (((ticks.toDouble() / 20.0) / 60.0) / config.EffectRate).toInt()
        }

        /**
         * 何秒間に飛ばすTick数が1上がるか
         */
//        private const val DayRate = 30
        fun skipDayTick(ticks: Long, config: ConfigManager): Double {
            return (ticks.toDouble() / 20.0) / config.DayRate
        }

        private val EFFECTS = arrayListOf(PotionEffectType.FAST_DIGGING/*, PotionEffectType.SPEED*/)

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

    fun save(){
        plugin.config.set("Range",Range)
        plugin.config.set("DayRate",DayRate)
        plugin.config.set("EffectRate",EffectRate)
        plugin.config.set("SpeedRate",SpeedRate)
        plugin.saveConfig()
    }
}