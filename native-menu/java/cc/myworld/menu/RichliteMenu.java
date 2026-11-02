package cc.myworld.menu;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-owned vanilla chest menus. No custom Screen, textures or GUI renderer. */
@Mod(RichliteMenu.ID)
public final class RichliteMenu {
    public static final String ID = "richlite_menu";
    static final String WEB = "https://xn--rhqx00c95nv9a.cc";
    static final UUID OWNER = UUID.fromString("85a880e8-a408-4fe5-b7d1-1545b1f88b08");
    private static final ConcurrentLinkedQueue<Runnable> ACTIONS = new ConcurrentLinkedQueue<>();
    private static final Map<UUID,Integer> LAST_OPEN = new HashMap<>();

    public RichliteMenu(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(RichliteMenu::commands);
        NeoForge.EVENT_BUS.addListener(RichliteMenu::tick);
        NeoForge.EVENT_BUS.addListener(RichliteMenu::login);
    }

    private static void commands(RegisterCommandsEvent event) {
        for (String name : new String[]{"menu", "servermenu"}) {
            event.getDispatcher().register(Commands.literal(name).requires(s -> s.getEntity() instanceof ServerPlayer)
                .executes(c -> { open(c.getSource().getPlayerOrException(), "main", 0); return Command.SINGLE_SUCCESS; }));
        }
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("[我的世界.cc] 按 Backspace 打开全局菜单，也可输入 /menu。").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void tick(ServerTickEvent.Post event) {
        Runnable action;
        int budget = 100;
        while (budget-- > 0 && (action = ACTIONS.poll()) != null) {
            try { action.run(); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger(ID).warn("Menu action failed", e); }
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 100 == 0) {
            try {
                JsonObject root = new JsonObject();
                root.addProperty("fresh", true); root.addProperty("at", Instant.now().toString());
                JsonArray players = new JsonArray();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    JsonObject row = new JsonObject(); row.addProperty("uuid", p.getUUID().toString());
                    row.addProperty("name", p.getGameProfile().getName()); row.addProperty("health", p.getHealth());
                    row.addProperty("dimension", p.level().dimension().location().toString()); players.add(row);
                }
                root.add("players", players);
                Path folder = server.getServerDirectory().resolve("richlite"); Files.createDirectories(folder);
                Path temp = folder.resolve("players.json.pending");
                Files.writeString(temp, new Gson().toJson(root), StandardCharsets.UTF_8);
                Files.move(temp, folder.resolve("players.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) { org.slf4j.LoggerFactory.getLogger(ID).warn("Player snapshot failed: {}", e.toString()); }
        }
    }

    static boolean administrator(ServerPlayer p) { return p.getUUID().equals(OWNER) || p.hasPermissions(2); }

    public static void open(ServerPlayer p, String page, int offset) {
        if (p.hasDisconnected() || !p.isAlive() || p.isSpectator()) return;
        int tick = p.server.getTickCount();
        if (tick - LAST_OPEN.getOrDefault(p.getUUID(), -100) < 3) return;
        LAST_OPEN.put(p.getUUID(), tick);
        p.closeContainer();
        String title = switch(page) {
            case "profile" -> "我的资料"; case "travel" -> "传送与主城"; case "land" -> "领地与队伍";
            case "play" -> "任务与玩法"; case "social" -> "在线玩家与社交"; case "finance" -> "余额与金融";
            case "web" -> "官网、下载与帮助"; case "status" -> "服务器状态"; case "admin" -> "管理员入口";
            default -> "服务器全局菜单";
        };
        p.openMenu(new SimpleMenuProvider((id, inventory, player) -> new NativeMenu(id, inventory, p, page, offset), Component.literal("我的世界.cc · " + title)));
    }

    static void link(ServerPlayer player, String path, String label) {
        player.closeContainer();
        player.sendSystemMessage(Component.literal("[我的世界.cc] ").withStyle(ChatFormatting.GOLD).append(
            Component.literal(label + "（点击打开）").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WEB + path)))));
    }

    static boolean available(ServerPlayer player, String command) {
        var node = player.server.getCommands().getDispatcher().getRoot();
        com.mojang.brigadier.tree.CommandNode<net.minecraft.commands.CommandSourceStack> current = node;
        for (String part : command.split(" ")) {
            var next = current.getChild(part);
            if (next == null) break;
            if (!next.canUse(player.createCommandSourceStack())) return false;
            current = next;
        }
        return current != node;
    }

    static void execute(ServerPlayer player, String command) {
        player.closeContainer();
        // Player command source retains permissions, cooldowns, claim checks and costs.
        player.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
    }

    public static final class NativeMenu extends ChestMenu {
        private final Map<Integer,Consumer<ServerPlayer>> actions = new HashMap<>();
        private final ServerPlayer owner;
        private boolean pending;
        private final SimpleContainer icons;

        NativeMenu(int id, Inventory inventory, ServerPlayer player, String page, int offset) {
            this(id, inventory, player, page, offset, new SimpleContainer(54));
        }
        private NativeMenu(int id, Inventory inventory, ServerPlayer player, String page, int offset, SimpleContainer container) {
            super(MenuType.GENERIC_9x6, id, inventory, container, 6);
            owner = player; icons = container;
            for (int i=0;i<54;i++) if (i<9 || i>=45 || i%9==0 || i%9==8) icon(i,Items.GRAY_STAINED_GLASS_PANE," ",null);
            icon(4,Items.GRASS_BLOCK,"我的世界.cc",null,"全服统一入口 · Backspace / /menu");
            icon(49,Items.BARRIER,"关闭菜单",ServerPlayer::closeContainer,"返回游戏");
            if (!page.equals("main")) icon(45,Items.ARROW,"返回主菜单",p -> open(p,"main",0));
            switch(page) {
                case "profile" -> {
                    icon(13,Items.PLAYER_HEAD,player.getGameProfile().getName(),null,"UUID："+player.getUUID(),"维度："+player.level().dimension().location(),"生命："+Math.round(player.getHealth())+" · 等级："+player.experienceLevel);
                    icon(29,Items.BOOK,"官网个人中心",p->link(p,"/account","打开个人中心"),"LittleSkin 角色与社区资料");
                    icon(33,Items.GOLD_INGOT,"余额与金融",p->open(p,"finance",0));
                }
                case "travel" -> {
                    command(11,Items.BEACON,"返回主城／出生点","spawn","沿用本服传送规则与冷却");
                    command(13,Items.RED_BED,"回家","home","沿用已设置的家与冷却");
                    command(15,Items.ENDER_PEARL,"返回上一地点","back","沿用服务器权限与传送规则");
                    icon(29,Items.OAK_SIGN,"设置家与传送帮助",p->{p.closeContainer();p.sendSystemMessage(Component.literal("使用 /sethome <名称> 设置家，/home <名称> 返回。传送请求可在在线玩家菜单发起。"));},"查看原有命令用法");
                    icon(33,Items.PLAYER_HEAD,"传送到玩家",p->open(p,"social",0),"向在线玩家发起 TPA 请求");
                }
                case "land" -> {
                    command(11,Items.MAP,"领取当前区块","ftbchunks claim","使用 FTB Chunks 原有领地规则");
                    icon(13,Items.COMPASS,"领地地图与管理说明",p->{p.closeContainer();p.sendSystemMessage(Component.literal("使用 FTB Chunks 地图（默认 M）查看领地；区块领取与取消仍受队伍和公共保护规则约束。"));});
                    command(15,Items.WHITE_BANNER,"队伍资料","ftbteams party info","查看你所属的 FTB 队伍");
                    icon(31,Items.WRITABLE_BOOK,"队伍与取消领地帮助",p->{p.closeContainer();p.sendSystemMessage(Component.literal("使用 /ftbteams 查看队伍命令；取消自己领地使用 /ftbchunks unclaim。请先确认当前位置与队伍。"));});
                }
                case "play" -> {
                    command(11,Items.BOOK,"服务器任务书","ftbquests open_book","查看本服已部署的任务与指引");
                    icon(13,Items.CRAFTING_TABLE,"工业、魔法与探索",p->link(p,"/mods","整合包玩法说明"));
                    icon(15,Items.WRITABLE_BOOK,"规则与新手帮助",p->link(p,"/rules","服务器规则"));
                    icon(31,Items.PAPER,"城市日报与公告",p->link(p,"/announcements","服务器公告"));
                }
                case "social" -> {
                    var players = player.server.getPlayerList().getPlayers().stream().filter(p -> p!=player).toList();
                    for(int i=offset;i<Math.min(players.size(),offset+21);i++) {
                        ServerPlayer target=players.get(i); int index=i-offset; int slot=10+(index/7)*9+index%7;
                        icon(slot,Items.PLAYER_HEAD,target.getGameProfile().getName(),p->{if(!target.hasDisconnected())execute(p,"tpa "+target.getGameProfile().getName());},"点击发出传送请求 · 由对方确认");
                    }
                    if(players.isEmpty())icon(22,Items.PAPER,"暂无其他在线玩家",null);
                    command(39,Items.LIME_DYE,"接受传送请求","tpaccept","接受最近的请求");
                    command(41,Items.RED_DYE,"拒绝传送请求","tpdeny","拒绝最近的请求");
                    if(offset>0)icon(47,Items.ARROW,"上一页",p->open(p,"social",Math.max(0,offset-21)));
                    if(offset+21<players.size())icon(51,Items.ARROW,"下一页",p->open(p,"social",offset+21));
                }
                case "finance" -> {
                    if(available(player,"jinyuan"))command(13,Items.GOLD_INGOT,"金圆券服务","jinyuan","以实际已启用的业务为准");
                    else icon(13,Items.GRAY_DYE,"金融业务尚未正式接入",null,"此处不展示虚构余额或资产");
                    icon(29,Items.BOOK,"我的游戏业务",p->link(p,"/my-business","我的游戏业务"));
                    icon(33,Items.EMERALD,"城市服务与公开行情",p->link(p,"/services","城市服务"));
                }
                case "web" -> {
                    icon(11,Items.GRASS_BLOCK,"官方网站",p->link(p,"/","我的世界.cc"));
                    icon(13,Items.CHEST,"客户端下载",p->link(p,"/download","两档客户端与更新"));
                    icon(15,Items.WRITABLE_BOOK,"公告与规则",p->link(p,"/rules","服务器规则"));
                    icon(29,Items.OAK_SIGN,"玩家社区",p->link(p,"/forum","玩家社区"));
                    icon(31,Items.PAPER,"反馈与申诉",p->link(p,"/feedback","反馈与申诉"));
                    icon(33,Items.BOOK,"个人中心",p->link(p,"/account","个人中心"));
                }
                case "status" -> {
                    icon(13,Items.CLOCK,"服务器运行中",null,"在线："+player.server.getPlayerCount()+" / "+player.server.getMaxPlayers(),"游戏版本：1.21.1 · NeoForge 21.1.251","每日维护：03:40–05:30 JST (UTC+09:00)");
                    icon(31,Items.REDSTONE_TORCH,"官网运行状态",p->link(p,"/about","服务器状态与介绍"));
                }
                case "admin" -> {
                    if(administrator(player)) {
                        icon(13,Items.COMMAND_BLOCK,"服务器管理后台",p->link(p,"/admin","打开管理后台"),"需使用获授权的官网账号登录");
                        icon(31,Items.CLOCK,"服务器状态",p->open(p,"status",0));
                    }
                }
                default -> {
                    icon(10,Items.PLAYER_HEAD,"我的资料",p->open(p,"profile",0),"角色、个人中心与余额入口");
                    icon(12,Items.COMPASS,"传送与主城",p->open(p,"travel",0),"主城、回家、返回与玩家传送");
                    icon(14,Items.MAP,"领地与队伍",p->open(p,"land",0),"原有 FTB 领地与队伍功能");
                    icon(16,Items.BOOK,"任务与玩法",p->open(p,"play",0),"任务书、玩法指南与服务器规则");
                    icon(28,Items.OAK_SIGN,"在线玩家与社交",p->open(p,"social",0),"在线玩家与 TPA 请求");
                    icon(30,Items.GOLD_INGOT,"余额与金融",p->open(p,"finance",0),"以实际已开放业务为准");
                    icon(32,Items.WRITABLE_BOOK,"官网、下载与帮助",p->open(p,"web",0),"官网、客户端、社区与反馈");
                    icon(34,Items.CLOCK,"服务器状态",p->open(p,"status",0),"在线人数与维护时间");
                    if(administrator(player))icon(47,Items.COMMAND_BLOCK,"管理员入口",p->open(p,"admin",0),"按官网账号权限管理");
                }
            }
        }
        private void command(int slot, Item item, String name, String command, String... lore) {
            if(available(owner,command))icon(slot,item,name,p->execute(p,command),lore);
            else icon(slot,Items.GRAY_DYE,name+" · 暂不可用",null,"此功能尚未启用，或当前角色没有权限");
        }
        private void icon(int slot, Item item, String name, Consumer<ServerPlayer> action, String... lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME,Component.literal(name).withStyle(Style.EMPTY.withItalic(false).withColor(action==null?ChatFormatting.GRAY:ChatFormatting.GOLD)));
            stack.set(DataComponents.LORE,new ItemLore(java.util.Arrays.stream(lore).map(s->Component.literal(s).withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))).map(c->(Component)c).toList()));
            icons.setItem(slot,stack);if(action!=null)actions.put(slot,action);
        }
        @Override public void clicked(int slot, int button, ClickType type, Player player) {
            // Reject QUICK_MOVE, SWAP, THROW, CLONE, QUICK_CRAFT and PICKUP_ALL,
            // including attempts against the player's inventory below the menu.
            if(player!=owner || pending || type!=ClickType.PICKUP || button!=0 || slot<0 || slot>=54) return;
            Consumer<ServerPlayer> action=actions.get(slot);
            if(action!=null){pending=true;ACTIONS.add(()->{pending=false;if(!owner.hasDisconnected()&&owner.containerMenu==this)action.accept(owner);});}
        }
        @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
        @Override public boolean stillValid(Player p){return p==owner&&p.isAlive();}
    }
}
