# Behavior trees in Citizens (as used by the Rival)

Study notes for this project. Everything here maps to real code in
[`src/main/java/com/github/denmeh/npcaitest`](../src/main/java/com/github/denmeh/npcaitest).

Citizens AI is **behavior trees** plus a **Navigator**. It is *not* GOAP: there is no planner, nothing
searches for a sequence of actions. The word "Goal" in Citizens means "a task the scheduler can pick",
not "Goal-Oriented Action Planning".

## 1. The two layers

```mermaid
flowchart TD
  subgraph scheduler [GoalController: picks ONE thing to run]
    prio4["priority 4: BodyCheck"]
    prio3["priority 3: GuardNet"]
    prio2["priority 2: Sequence(chase, strike)"]
    prio1["priority 1: IdleBehavior"]
  end
  subgraph nav [Navigator: pathfinding only]
    path["setTarget(location) then walks over many ticks"]
  end
  prio4 --> nav
  prio3 --> nav
  prio2 --> nav
  prio1 --> nav
```

- **`GoalController`** (`npc.getDefaultGoalController()`) runs every tick and picks the
  **highest-priority behavior whose `shouldExecute()` is true**. Only one runs at a time.
- **`Behavior`** is the tree node. Leaves extend `BehaviorGoalAdapter`; composites
  (`Sequence`, `Selector`, `IfElse`) combine them.
- **`Navigator`** only walks. It knows nothing about the tree. You set a target **once**
  and it paths there over many ticks.

## 2. Node lifecycle

Every node is a tiny state machine with three methods:

| Method | When Citizens calls it | What it is for |
|---|---|---|
| `shouldExecute()` | **once**, when the parent is choosing a child | "Can I run right now?" |
| `run()` | **every tick**, while the node returns `RUNNING` | do the work, report status |
| `reset()` | when the node stops running | clear state, cancel navigation |

```mermaid
stateDiagram-v2
  [*] --> Considered
  Considered --> Running: shouldExecute() == true
  Considered --> [*]: shouldExecute() == false
  Running --> Running: run() == RUNNING
  Running --> Done: run() == SUCCESS or FAILURE
  Done --> [*]: reset()
```

The status returned by `run()` drives everything:

| Status | Meaning |
|---|---|
| `RUNNING` | Not finished; call me again next tick |
| `SUCCESS` | Finished well; the parent may advance |
| `FAILURE` | Failed; a `Sequence` aborts, a `Selector` can try another child |
| `RESET_AND_REMOVE` | Parent composite should drop this child entirely |

**The trap:** `shouldExecute()` runs *once*, not every tick. Anything that changes while the node runs
(the puck moving, a cooldown expiring) must be re-checked inside `run()`.

## 3. Composites

- **`Sequence.createSequence(a, b)`** — run `a`, then `b`. First `FAILURE` aborts the whole sequence.
- **`Selector.selecting(a, b).build()`** — pick **one** child (random by default; use
  `Selectors.prioritySelector` for ordered).
- **`IfElse.create(cond, ifNode, elseNode)`** — condition evaluated in `shouldExecute()`.
- Decorators wrap a single child: `TimerDecorator.tickLimiter`, `TimeoutDecorator`,
  `RetryDecorator`, `InverterDecorator`, `Precondition`, `Loop`, `Callback`.

### Sequence semantics, which bit us twice

`Sequence` is strict: a child returning `FAILURE` kills the run, and the tree restarts from the top
next tick. Two real bugs in this project came from that:

1. `StrikeTowardGoal` returned `FAILURE` while its attack cooldown was still ticking. The sequence
   aborted, `reset()` cancelled navigation, and the Rival stood next to the puck doing nothing.
   **Fix:** return `RUNNING` while waiting, not `FAILURE`.
2. `StrikeTowardGoal` returned `FAILURE` when the shooting angle was briefly wrong. It never got the
   chance to fix its position, so it circled forever without shooting.
   **Fix:** stay `RUNNING` and *steer* while lining up.

Rule of thumb: **`FAILURE` means "this is impossible now", not "I am not done yet".**

### Why role selection is not a `Selector`

A textbook BT would put the Rival's three roles under a priority `Selector`. Citizens' `Selector` cannot
do that job, for two reasons worth knowing:

1. **It is not reactive.** It picks a child *once*, then runs it until that child returns `SUCCESS` or
   `FAILURE`. A chase that stays `RUNNING` for four seconds cannot be interrupted by a role that just
   became more important.
2. **Priority selection does not fall through.** `Selectors.PrioritySelection` sorts the children and
   returns the last one *without consulting `shouldExecute()`*. If that child cannot run, `Selector`
   returns `FAILURE` for the whole node instead of trying the next-best child. And in 2.0.40
   `Selectors.prioritySelector` is outright broken: its guard reads `if (behaviors.size() > 0) throw`,
   so it throws whenever you pass it any behaviors at all.

`GoalController` already *is* a reactive priority selector: it re-checks priorities every tick and
preempts whatever is running. So roles are registered as priorities, and composites (`Sequence`,
`Precondition`) are used *within* a role where the reactivity is not needed. Same tree shape, working
preemption.

## 4. The Rival's tree

```mermaid
flowchart TD
  gc{GoalController each tick}
  gc -->|"priority 4<br/>you carry the puck, he is close"| check["BodyCheck<br/>charge and shove"]
  gc -->|"priority 3<br/>you carry the puck at his end"| guard["GuardNet<br/>play goalie"]
  gc -->|"priority 2<br/>otherwise"| pre["Precondition<br/>live play and puck alive"]
  gc -->|"priority 1, fallback"| idle["IdleBehavior<br/>stand still"]

  pre --> seq["Sequence"]
  seq --> chase["ChaseToIntercept"]
  chase -->|"puck gone: FAILURE"| gc
  chase -->|"within 2.6 blocks: SUCCESS"| strike["StrikeTowardGoal"]

  strike -->|"not lined up and not pressured: RUNNING"| orbit["skate around the puck"]
  orbit --> strike
  strike -->|"cooldown: RUNNING"| strike
  strike -->|"hit taken: SUCCESS"| gc
  strike -->|"puck left range: FAILURE"| gc

  check -->|"shoved: SUCCESS"| gc
  check -->|"40 ticks, no contact: FAILURE"| gc
  guard -->|"puck within reach: SUCCESS"| gc
  guard -->|"you lost the puck: FAILURE"| gc
```

| Node | File | Job |
|---|---|---|
| `BodyCheck` | [BodyCheck.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/BodyCheck.java) | Charge the player carrying the puck and shove them off it |
| `GuardNet` | [GuardNet.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/GuardNet.java) | Drop back onto the line between his net and the puck |
| `ChaseToIntercept` | [ChaseToIntercept.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/ChaseToIntercept.java) | Skate to the attack stance or the defensive block point |
| `StrikeTowardGoal` | [StrikeTowardGoal.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/StrikeTowardGoal.java) | Circle into position, then swing |
| `IdleBehavior` | [IdleBehavior.java](../src/main/java/com/github/denmeh/npcaitest/ai/IdleBehavior.java) | Fallback: puck respawning, or a faceoff freeze |
| `RivalContext` | [RivalContext.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/RivalContext.java) | Shared geometry, aiming, look control, shot plan |
| `SkateTo` | [SkateTo.java](../src/main/java/com/github/denmeh/npcaitest/arena/ai/SkateTo.java) | Shared "path far, steer near" movement, one instance per node |

`RivalContext` is deliberately **not** a node. Leaves stay small and readable; all the hockey maths
(where to stand, where to aim, how to turn the head) lives in one place every leaf can query.

### Contesting the player

The first version of the tree only knew about the puck, which made the Rival feel like a ball machine:
it never reacted to *you*. Two roles fixed that, both keyed on `playerControlsPuck()` — you are within
3.6 blocks of the puck **and** closer to it than he is.

- **`GuardNet`** stops him charging a puck he cannot win. He retreats to `goaliePoint()`, 3.2 blocks off
  his own goal line on the line out toward the predicted puck spot, and waits facing the puck. It stands
  down (`shouldExecute` false) once the puck is within his reach, otherwise it would keep returning
  `SUCCESS` on the spot and starve the attack branch that is meant to clear it.
- **`BodyCheck`** charges you and shoves you off the puck. The shove is `setVelocity`, **not**
  `player.attack` — it moves you without touching your health. It runs on a ~5 s cooldown started in
  `reset()`, so a charge that never lands is not retried instantly.

There is also a defensive tweak inside `StrikeTowardGoal`: if you are within 3.2 blocks
(`ctx.pressured()`) he skips the skate-around and shoots immediately, rather than being stripped while
fussing over the perfect angle.

### Freezing the tree between plays

Every role checks `ctx.playable()`, which is just `arena.playing()`. During a goal celebration or a
faceoff countdown that is false, so priorities 4-2 all decline and `IdleBehavior` takes over: the Rival
stands on his faceoff dot until the puck drops. One flag freezes the whole tree, with no node needing to
know what a faceoff is.

### Attack vs defend

Decided **inside `run()`**, every tick, so it can flip mid-skate:

- Puck closer to the **red / rival** net → skate **between the puck and that net** (defend).
- Otherwise → skate **behind the puck** relative to the blue player net (attack).

Both are judged on the *predicted* puck position, not the current one, so the Rival commits to
defending before the puck actually arrives.

### Puck prediction (leading the puck)

Skating to where the puck **is** means always arriving late: by the time the Rival gets there, the
puck has slid on. `interceptPuckLocation()` solves for where to meet it instead.

The puck is simulated forward with per-tick drag, then the first reachable point wins:

```java
for (int ticks = 0; ticks <= 30; ticks += 2) {
    best = puckAfter(ticks);                                  // simulate the slide
    if (distanceTo(best) / RIVAL_TICK_SPEED <= ticks) {        // can we be there in time?
        break;
    }
}
```

```mermaid
flowchart LR
  P0["puck now"] -->|"drag 0.96 per tick"| P1["+10 ticks"]
  P1 --> P2["+20 ticks"]
  P2 --> P3["+30 ticks"]
  R["rival"] -.->|"too far, arrives late"| P1
  R ==>|"first reachable: skate here"| P2
```

Details that matter:

- **Drag**: a turtle sliding on ice keeps ~96% of its speed per tick, so the path curves to a stop
  rather than running forever.
- **Boards**: the simulated position is clamped inside the rink, so predictions never point through a wall.
- **Slow puck**: below 0.08 blocks/tick the prediction is skipped and the current position is used,
  otherwise the target would jitter while the puck sits still.
- **Horizon**: capped at 30 ticks (1.5 s) so he never commits to an absurd far-future spot.

`RIVAL_TICK_SPEED` (0.3 blocks/tick) is the tuning knob: raise it and he leads the puck further,
believing he is faster than he is.

### The skate-around ("giro")

The Rival must be *behind* the puck to shoot it forward. Steering straight to that spot walks through
the puck, which is a solid turtle. So `orbitPoint()` returns **one 45-degree arc step** around the
puck at a 2.2 block radius, recomputed each tick. Repeated ticks trace a circle.

```mermaid
flowchart LR
  A["rival on the wrong side"] --> B["arc step 45 deg"]
  B --> C["arc step 45 deg"]
  C --> D["behind the puck: aligned"]
  D --> E["swing"]
```

Guards against getting stuck:

- Puck pinned to the boards where getting behind it is impossible: after ~45 ticks, **shoot anyway**.
- Puck buried in a corner: aim at **center ice** instead of the net, which clears it into open play.

## 5. Navigator vs direct movement

Two different movement tools, used for different jobs:

| Tool | Use | Rule |
|---|---|---|
| `navigator.setTarget(loc)` | Long skate across the rink; real pathfinding | Set it **once per destination**, never every tick |
| `npc.setMoveDestination(loc)` | Short, precise steering (orbit, closing in) | Straight line, **must** be called every tick |

`SkateTo` implements that switch once and every travelling node uses it: beyond 5 blocks it pathfinds
(and only re-paths when the target moved more than 0.6 blocks, at most every 4 ticks, because a
predicted target moves constantly); inside 5 blocks it cancels navigation and steers in a straight
line, which tracks a moving puck far better. `StrikeTowardGoal` always steers directly for the tight
circling. Each node owns its own `SkateTo` instance, because the last target is per-node state.

Useful `NavigatorParameters` (see [RivalNpc.java](../src/main/java/com/github/denmeh/npcaitest/arena/RivalNpc.java)):

| Parameter | Why it matters here |
|---|---|
| `speedModifier(1.75)` | Citizens has no "sprint"; speed is how you fake it |
| `distanceMargin(0.75)` | Default is **2 blocks**, so the NPC stopped short and could never reach melee range |
| `lookAtFunction(...)` | Overrides where the NPC looks while pathing |

### Look control

Two systems both wanted to turn the head (`lookAtFunction` and our own `faceLocation`), which caused
the jittery, sky-staring pivot. Now `RivalContext.lookTarget()` is the single source of truth:
both call it, the yaw turns at a capped rate per tick, and the target is kept **level with the feet**
so pitch never cranes up or down.

## 6. Adding a new leaf

```java
public final class MyLeaf extends BehaviorGoalAdapter {

    private final RivalContext ctx;

    @Override
    public boolean shouldExecute() {
        return ctx.spawned() && ctx.puckAlive();   // cheap check, called once
    }

    @Override
    public BehaviorStatus run() {
        if (impossibleNow()) {
            return BehaviorStatus.FAILURE;         // genuinely cannot continue
        }
        if (stillWorking()) {
            return BehaviorStatus.RUNNING;         // includes "waiting for a cooldown"
        }
        return BehaviorStatus.SUCCESS;
    }

    @Override
    public void reset() {
        // clear per-run state; cancel navigation if this leaf started it
    }
}
```

Then decide where it belongs. A step *within* an existing role goes into that role's composite; a whole
new role gets its own priority, above the roles it should be able to interrupt:

```java
// a new step inside the attack role
controller.addBehavior(Precondition.wrappingPrecondition(
        Sequence.createSequence(new ChaseToIntercept(ctx), new MyLeaf(ctx)),
        () -> ctx.playable() && ctx.puckAlive()), 2);

// a new role that can preempt attacking
controller.addBehavior(new MyRole(ctx), 5);
```

`Precondition.wrappingPrecondition(child, cond)` is the decorator to reach for when a subtree should be
skipped wholesale: its `shouldExecute()` is `cond && child.shouldExecute()`. Without it a `Sequence`
always claims its priority, because `Composite.shouldExecute()` only checks that it *has* children.

## 7. Debugging

`TestNpc.setActiveNode(...)` records which node is live; `/npctest status` and the action bar show it.

| Label | Meaning |
|---|---|
| `CHASE_ATTACK` | Skating behind a settled puck to shoot it |
| `CHASE_LEAD` | Puck is sliding; skating to the predicted intercept |
| `CHASE_DEFEND` | Getting between a settled puck and the red net |
| `DEFEND_LEAD` | Cutting off a puck sliding toward the red net |
| `SKATE_AROUND` | In range but wrong side; circling the puck |
| `STRIKE` | Lined up; swinging or waiting out the swing cooldown |
| `GUARD_NET` | You have the puck at his end; he is sitting in front of his net |
| `BODY_CHECK` | Charging you to shove you off the puck |
| `FACEOFF` | Parked on his dot during the countdown |
| `IDLE` | Fallback, usually while the puck respawns |

When behaviour looks wrong, read that label first: it tells you whether the tree picked the wrong node
or the right node is doing the wrong thing.

## 8. Sources

- [Citizens API wiki](https://wiki.citizensnpcs.co/API)
- [`net.citizensnpcs.api.ai.tree` javadocs](https://jd.citizensnpcs.co/net/citizensnpcs/api/ai/tree/package-summary.html)
- [`NavigatorParameters` javadocs](https://jd.citizensnpcs.co/net/citizensnpcs/api/ai/NavigatorParameters.html)
