package me.ripex.gsnstick

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class GSN_Stick : JavaPlugin(), Listener {

    private val playersFrozen = mutableSetOf<String>()
    private val stickName = Component.text("Полицейская дубинка", NamedTextColor.GOLD)

    override fun onEnable() {
        logger.info("Ого я палка!")
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        logger.info("О не я больше не палка!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("gsn", true)) {
            if (sender is Player) {
                val player = sender
                if (args.isEmpty()) {
                    player.sendMessage(Component.text("Используйте команду /gsn stick для создания специальной палки.", NamedTextColor.YELLOW))
                    return true
                }
                if (args.isNotEmpty() && args[0].equals("stick", true)) {
                    if (player.hasPermission("gsnstick.use")) {
                        val itemInHand = player.inventory.itemInMainHand
                        if (itemInHand.type == Material.STICK) {
                            val meta = itemInHand.itemMeta
                            meta?.displayName(stickName)

                            // Добавляем уникальный NBT тег
                            val key = NamespacedKey(this, "unique_id")
                            meta?.persistentDataContainer?.set(key, PersistentDataType.STRING, "gsn_stick_unique_id")

                            itemInHand.itemMeta = meta
                            player.sendMessage(Component.text("Вы создали дубинку!", NamedTextColor.GREEN))
                        } else {
                            player.sendMessage(Component.text("Вы должны держать палку в руке!", NamedTextColor.RED))
                        }
                    } else {
                        player.sendMessage(Component.text("У вас нет разрешения на использование этой команды!", NamedTextColor.RED))
                    }
                    return true
                }
            }
        }
        return false
    }

    @EventHandler
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val stunText = Component.text("Вас оглушили", NamedTextColor.RED)
        if (damager is Player) {
            val item = damager.inventory.itemInMainHand
            if (item.type == Material.STICK && item.hasItemMeta()) {
                val meta = item.itemMeta
                val key = NamespacedKey(this, "unique_id")
                if (meta?.persistentDataContainer?.has(key, PersistentDataType.STRING) == true &&
                    meta.persistentDataContainer.get(key, PersistentDataType.STRING) == "gsn_stick_unique_id") {
                    val entity = event.entity
                    if (entity is Player) {
                        val curName = entity.name
                        playersFrozen.add(curName)
                        // Сообщаем игроку, что он заморозил другого игрока
                        damager.sendMessage(Component.text("Вы заморозили игрока $curName!", NamedTextColor.YELLOW))
                        entity.sendActionBar(stunText)
                        entity.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 80, 1))
                        entity.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 80, 255))
                        // Отменяем задачу через 4 секунды
                        Bukkit.getScheduler().runTaskLater(this, Runnable {
                            playersFrozen.remove(curName)
                            damager.sendMessage(Component.text("Игрок $curName разморозился!", NamedTextColor.YELLOW))
                        }, 80L) // 20 тиков в секунде, 80 тиков = 4 секунды
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val playerName = event.player.name
        val blockUnderPlayerFeet: Block = player.location.subtract(0.0, 1.0, 0.0).block
        if (playersFrozen.contains(playerName)) {
            if (blockUnderPlayerFeet.type != Material.AIR) {
                player.teleport(Location(player.world, player.location.x, blockUnderPlayerFeet.location.y + 1.0, player.location.z))
            }
        }
    }
}
