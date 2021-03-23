package net.kunmc.lab.getfastereachday

import net.kunmc.lab.getfastereachday.flylib.SmartTabCompleter
import net.kunmc.lab.getfastereachday.flylib.TabChain
import net.kunmc.lab.getfastereachday.flylib.TabObject
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable

class GetFasterEachDay : JavaPlugin() {
    lateinit var manager: FasterManager
    lateinit var conf:ConfigManager
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
                    sender.sendMessage("Now Effect Level:${FasterManager.effectLevel(plugin.manager.TickCount,plugin.conf)}")
                    sender.sendMessage("Now Skip Day Tick:${FasterManager.skipDayTick(plugin.manager.TickCount,plugin.conf)}")
                }
                "c", "change" -> {
                    plugin.manager.change()
                    sender.sendMessage("Now Entity Boosted Value:${plugin.manager.EntityBoosted}")
                }
                else -> {
                    return false
                }
            }
            return true
        } else {
            return false
        }
    }
}

class FasterManager(val plugin: GetFasterEachDay) : BukkitRunnable() {
    init {
        this.runTaskTimer(plugin, 1, 1)
    }

    var isGoingOn = false
        private set

    var TickCount:Long = 0
        private set

    var EntityBoosted = false
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

    private fun removeEffect(){
        Bukkit.getOnlinePlayers().forEach { p ->
            EFFECTS.forEach { e->
                p.removePotionEffect(e)
            }
            if(EntityBoosted){
                p.getNearbyEntities(plugin.conf.Range,plugin.conf.Range,plugin.conf.Range)
                    .filter { it is LivingEntity }
                    .forEach {
                    EFFECTS.forEach { e->
                        (it as LivingEntity).removePotionEffect(e)
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


            val effectLevel = effectLevel(TickCount,plugin.conf)
            if (effectLevel > 0) {
                Bukkit.getOnlinePlayers().forEach { p ->
                    EFFECTS.forEach {
                        p.addPotionEffect(PotionEffect(it, Int.MAX_VALUE, effectLevel))
                    }

                    if (EntityBoosted) {
                        p.getNearbyEntities(plugin.conf.Range, plugin.conf.Range, plugin.conf.Range)
                            .filter { it is LivingEntity }
                            .forEach { e ->
                                EFFECTS.forEach {
                                    (e as LivingEntity).addPotionEffect(PotionEffect(it, Int.MAX_VALUE, effectLevel))
                                }
                            }
                    }
                }
            }
            if (effectLevel != lastEffectLevel) {
                Bukkit.broadcastMessage("エフェクトのレベルが${effectLevel+1}に上がった!")
                lastEffectLevel = effectLevel
            }

            val skipTick = skipDayTick(TickCount,plugin.conf)
            skipStack += skipTick
            val skip = skipStack.toInt()
            if(skip >= 1){
                skipStack -= skip
                Bukkit.getWorlds().forEach { it.time += skip }
                val displaySkippedTick = (skipTick * 10).toInt().toDouble() / 10
                if (displaySkippedTick != lastSkipTick) {
                    Bukkit.broadcastMessage("現在${displaySkippedTick+1}倍速!")
                    lastSkipTick = displaySkippedTick
                }
            }

        }
    }

    companion object {
        /**
         * 何分間に1レベル上がるか
         */
//        private const val EffectRate = 1
        fun effectLevel(ticks: Long, config:ConfigManager): Int {
            return (((ticks.toDouble() / 20.0) / 60.0) / config.EffectRate).toInt()
        }

        /**
         * 何秒間に飛ばすTick数が1上がるか
         */
//        private const val DayRate = 30
        fun skipDayTick(ticks: Long, config:ConfigManager): Double {
            return (ticks.toDouble() / 20.0) / config.DayRate
        }

        private val EFFECTS = arrayListOf(PotionEffectType.FAST_DIGGING, PotionEffectType.SPEED)

//        private val Range = 10.0
    }
}

class ConfigManager(plugin:GetFasterEachDay){
    val Range = plugin.config.getDouble("Range")
    val DayRate = plugin.config.getInt("DayRate")
    val EffectRate = plugin.config.getDouble("EffectRate")
}