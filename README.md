# NpcAiTest

A Paper/Spigot sandbox for **Citizens NPC AI**, focused on the Citizens **behavior tree** API (`net.citizensnpcs.api.ai.tree`). It is not GOAP. Citizens “Goal” is a priority scheduler; new NPC logic should be `Behavior` nodes composed with `Sequence`, `Selector`, and `IfElse`.

The ice rink is a second test bed: a temporary hockey minigame. **Rival plays with a behavior tree** — four roles on the `GoalController` (body check, guard net, attack, idle), with a `Sequence` inside the attack role. You score in the **red** net; Rival scores in the **blue** net.

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

Full write-up with diagrams: [docs/behavior-tree.md](docs/behavior-tree.md).

## Ice rink

`/npctest arena` pastes a packed-ice rink from a text schematic, snapshots your **inventory, gamemode, and location**, then:

- Sets **adventure**
- Gives **Knockback I / II** sticks in hotbar slots 1–2
- Puts **Leave Arena** (barrier) in the **last hotbar slot**
- Spawns a turtle puck and **Rival**, who skates, defends the red half, guards his net, and tries to knock the puck into **your** (blue) net

Left-click the turtle with a stick. Score in the **red** net; Rival scores in the **blue** net; first to 3. Leave with the barrier, `/npctest leave`, or quit: world blocks and your previous state are restored.

A match runs as a small state machine: **faceoff** (3-2-1 countdown, both skaters parked on their dots, puck pinned) → **play** → **celebration** (goal horn, title, sparks out of the net) → faceoff again. A boss bar carries the score. Goals only count during play, and the Rival's whole tree is frozen outside it.

The puck plays like a puck rather than a mob: your hits are scaled up past vanilla knockback, and it **rebounds off the boards** instead of stopping dead against them, since Minecraft entities do not bounce on their own. The bounce planes come from the schematic's open ice, with the net mouths excluded so shots can still go in.

Rival’s tree (registered on spawn, no LookClose):

| Priority | Node | When it runs |
|---|---|---|
| 4 | `BodyCheck` | You are carrying the puck and he is within 5 blocks |
| 3 | `GuardNet` | You are carrying the puck at his end |
| 2 | `Sequence(ChaseToIntercept, StrikeTowardGoal)` | Live play and the puck is alive |
| 1 | `IdleBehavior` | Otherwise, including faceoffs |

`ChaseToIntercept` **leads the puck**: it simulates the slide forward and skates to the first spot it can actually reach, either behind the puck (attack) or between the puck and the red net (defend). `StrikeTowardGoal` circles the puck until it can shoot forward, then swings — unless you are right on top of him, in which case he shoots straight away rather than get stripped. Each shot is re-rolled: a different spot inside your net, different power, and Knockback **I** for close taps or **II** for long clears.

`GuardNet` and `BodyCheck` are what make him play against *you* rather than against the puck: he stops chasing a puck he cannot win and sits in front of his net instead, and every few seconds he will charge and shove you off it (a velocity shove, so it never damages you).

The rink is pasted in **small batches each tick** (~192 blocks) so the build does not hitch the server. Layout lives in `plugins/NpcAiTest/arena/rink.txt`. Delete that file and reload to reset the bundled **19×33** 1v1 rink (packed/blue ice only — no melting ice or water).

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
