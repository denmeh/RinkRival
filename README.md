# NpcAiTest

A Paper/Spigot sandbox for **Citizens NPC AI**, focused on the Citizens **behavior tree** API (`net.citizensnpcs.api.ai.tree`). It is not GOAP. Citizens “Goal” is a priority scheduler; new NPC logic should be `Behavior` nodes composed with `Sequence`, `Selector`, and `IfElse`.

The ice rink is a second test bed: a temporary minigame where an NPC can later play. **Rival is idle today** (`IdleBehavior` + look-at-player).

## Requirements

- Minecraft **1.20.6** (Spigot/Paper API)
- **Citizens 2** (`citizens-main` 2.0.40+)
- Java **21**

```bash
mvn package
```

Drop `target/NpcAiTest-1.0-SNAPSHOT.jar` into `plugins/` next to Citizens.

## Behavior trees

`/npctest spawn` creates a temporary player NPC. `/npctest tree` registers two leaf nodes on `GoalController`:

| Priority | Node | When it runs |
|---|---|---|
| 2 | `FollowPlayerBehavior` | A player is within 12 blocks |
| 1 | `IdleBehavior` | Otherwise |

Leaves extend `BehaviorGoalAdapter` and return `RUNNING` / `SUCCESS` / `FAILURE`. Pathfinding uses `Navigator.setTarget` once, not every tick.

## Ice rink

`/npctest arena` pastes a packed-ice rink from a text schematic, snapshots your **inventory, gamemode, and location**, then:

- Sets **adventure**
- Gives **Knockback I / II** sticks in hotbar slots 1–2
- Puts **Leave Arena** (barrier) in the **last hotbar slot**
- Spawns a slime puck and an idle Rival

Left-click the puck with a stick. Score in the **red** net; first to 3. Leave with the barrier, `/npctest leave`, or quit: world blocks and your previous state are restored.

Layout lives in `plugins/NpcAiTest/arena/rink.txt` (copied from the jar on first run). Delete that file and reload to reset the bundled 21×42 rink. Legend is in the file header (`i` ice, `w` walls, `P`/`N`/`O` spawns, `g`/`e` nets).

## Commands

| Command | What it does |
|---|---|
| `/npctest spawn [name]` | Temporary Citizens NPC |
| `/npctest come` | `MoveToGoal` to you |
| `/npctest tree` | Follow vs idle tree |
| `/npctest status` | Active node + navigating |
| `/npctest remove` | Despawn that NPC |
| `/npctest arena` | Build rink and enter |
| `/npctest leave` | Exit rink and roll back |

Permission: `npctest.use` (default op).
