# RinkRival

1v1 hockey against a NPC.

## Requirements

- Minecraft **1.20.6** (Spigot/Paper API)
- **Citizens 2** (`citizens-main` 2.0.40+)
- Java **21**

```bash
mvn package
```

Drop `target/RinkRival-1.0-SNAPSHOT.jar` into `plugins/` next to Citizens. Remove any old `NpcAiTest` jar and data folder (`plugins/NpcAiTest`) so the rink schematic loads from `plugins/RinkRival/`.

## Play

`/rink arena` opens a chest menu: **Easy / Normal / Hard**. `/rink arena easy` (or `normal` / `hard`) skips the menu.

That pastes a packed-ice rink from a text schematic, snapshots your **inventory, gamemode, and location**, then:

- Adventure mode
- **Tap Stick** (Knockback I) and **Slap Stick** (Knockback II) in slots 1–2
- **Leave Arena** (barrier) on the last hotbar slot
- A turtle puck and a **random rival** (hockey name + dyed jersey)

**Shoot:** left-click the puck. **Check:** left-click the rival — vanilla stick knockback, no extra shove, they cannot die. Leave with the barrier, `/rink leave`, or quit: world blocks and your previous state come back.

You shoot the **red** net. He shoots **blue**. First to 3.

A match is **faceoff** (3-2-1, both on their dots, puck pinned) → **play** → **celebration** (horn, title, sparks) → faceoff. A boss bar holds the score. Goals and the rival's tree only run during play.

The puck slides with drag and **rebounds off the boards**. Net mouths are left out of the bounce planes so shots can go in. The floor is **blue ice** (`z` in the schematic) — regular ice would melt next to the sea lanterns.

## Rival

The rival is a skater, not a crease-camper. His tree lives in [`bt`](src/main/java/com/github/denmeh/rinkrival/bt) and is ticked by a single Citizens goal, because Citizens' own `Selector` cannot interrupt a running child.

```
Selector "rival"
├── Guard "check"   you are on the puck, he is close, puck out of his reach
│   └── Cooldown (by difficulty) → Timeout 40t → BodyCheck
├── Guard "block"   you are rushing with the puck and he is too far to steal
│   └── BlockLane
├── Guard "defend"  the puck is already a shot at his net
│   └── GuardNet
├── Guard "attack"  live play, puck alive, not stunned
│   └── Sequence "rush" → ChaseToIntercept → StrikeTowardGoal
└── Hold            stand still (faceoffs) or slide after a check
```

The `Selector` retries from the top every tick, so a higher branch interrupts a chase.

| Branch | What you see |
|---|---|
| `BlockLane` | Shades the rush from a distance, cheated off the shot line |
| `GuardNet` | Only on an incoming shot. Stands at a post with a far-post hole (`guardGap`). No skate-boost |
| `Chase` | Leads the puck. In his zone, still tries to steal, leaning a bit toward his net |
| `BodyCheck` | Charges and shoves **you** (velocity, no damage) |
| Player check | Your stick's vanilla knockback; his navigator pauses so pathfinding does not eat the hit |

Difficulty scales skate speed, shot power, miss chance, check cooldown, and how wide that far-post gap is. Easy is the leaky goalie; Hard stands tighter and shoots cleaner.

The rink pastes in small batches (~192 blocks/tick). Layout: `plugins/RinkRival/arena/rink.txt`. Delete that file and reload to restore the bundled **19×33** 1v1 rink.

Full tree write-up: [docs/behavior-tree.md](docs/behavior-tree.md).

## Commands

| Command | What it does |
|---|---|
| `/rink arena` | Difficulty menu, then paste a rink |
| `/rink arena <easy\|normal\|hard>` | Skip the menu |
| `/rink leave` | Exit and roll back |

Permission: `rinkrival.use` (default op).
