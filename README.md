# Avi's Unnamed Space Mod

## Code Overview
- A ton of the client-side rendering stuff is done via custom implementations of vanilla structures, like `BufferBuilder` and `RenderTarget`
- Custom math types that dont sucks so much to use: `Vec3`, `Vec3i`, `Quat`, `Color`, etc. These are located currently in `net/xavil/util/math`
- Procedural star system generate is split between `net/xavil/universal/common/universe/system/StarSystemGenerator` and the classes in `net/xavil/universal/common/universe/system/gen`. Currently, it is horribly, horribly broken. It started as a half-baked implementation of the ancient Accrete algorithm (aka Starform or Stargen) which I tried to modify to be more flexible, but I don't know astrophysics!

### `CelestialNode`
`CelestialNode` in `net/xavil/universegen/system` and its related classes are representations of celestial bodies and their children. Orbits are defined per-child, instead of per-node, meaning you have to use `CelestialNodeChild` to get orbital information about a celestial body.
- `CelestialNode`s form trees, and to enable efficient lootkups, each node in the tree is assigned an id that is greater than all of its children's ids. IDs are not assigned automatically as you insert nodes into the tree, so you must call `assignIds` on the root node before using them in certain places. Some operations do not actually need the tree IDs to be in place to work.
- Orbits are simple keplerian orbits, not a fully nbody gravity simulation, and can be derived solely from time, using the `updatePositions` method.
- Currently, `CelesialNode` has `readNbt` and `writeNbt` so that they can be serialized over the network.

### Networking
Some mixins in `ConnectionProtocol` fire an event that allows this mod to register custom network packets without the overhead of `ServerboundCustomPayloadPacket`, which needs to include an entire ass channel ID in every packet. I'm not sure how compatible these changes are with other mods, but I doubt it would be too terrible. It shouldn't mess with the ability to connect to vanilla servers either, since packet IDs are assigned linearly, and we insert new packet types after vanilla finishes adding its packet types. Packets can be registered with `ModNetworking.REGISTER_PACKETS_EVENT`.

There are currently 4 custom packet types:
- `ClientboundUniverseInfoPacket`
	- Sends basic universe info to the client, like the universe seed, which celestial node the client starts on, and the actual contents of that starting node.
	- This info is used primarily to sync `ClientUniverse` with `ServerUniverse` (more on that later)
- `ClientboundOpenStarmapPacket`
	- Tells the client to open the starmap GUI. Used to implement the starmap item's right click behavior.
- `ClientboundChangeSystemPacket`
	- Notifies the client that the celestial node they're on has changed, and as such things like the background stars need to be rebuilt.
- `ServerboundTeleportToPlanetPacket`
	- Debug/operator request to be teleported to the requested celestial node.

### IDs
- `SystemNodeId`
	- Full-resolution ID that allows identifying a single celestial node with no prior context.
- `SystemId`
	- Represents an entire star system with no prior context.
- `SectorId`
	- Represents a single element inside a `TicketedVolume`

### `TicketedVolume`
An infinite spatial data structure comprised of a 3d grid of octrees that store galaxies or systems inside. There's some jank here, since all of my code accesses this structure in an immediate mode sort of way, but the structure wants to know what sectors should be loaded and unloaded at any given time, but we just access it like `volume.get(...)`, which means it has to store temporary tickets for each lookup.

### `Galaxy`
Galaxies are responsible for a few things. They serve as context for generating star systems, as well as storing the results of system generation. Sectors within a galaxy are called "galaxy sectors".

### `Universe`
Like Minecraft's `Level`, `ServerLevel`, and `ClientLevel`, `Universe` is split between itself, `ServerUniverse`, and `ClientUniverse`. Like `Galaxy`, `Universe` serves as a context for generating galaxies, and also stores the results of that generation within itself. The generation process does not happen in the same way that star system generation does.

There are two types of seed: _common_ and _unique_. The _common_ seed is shared between everyone that uses the mod (this will be configurable in the future) and the _unique_ seed is unique per world. The _common_ seed drives the actual universe generation: the distribution of galaxies, the shape of those galaxies, the distribution of stars within those galaxies, and the layout of star systems. The _unique_ seed drives starting system placement. This means that for the default _common_ seed, two beings may share universe coordinates and be able to talk about the same planet/system/galaxy, even on worlds with different vanilla seeds.

`Universe` also provides a `StartingSystemGalaxyGenerationLayer`, which needs the galaxy sector, the position within that sector to place the starting system at, the actual `CelesialNode` that is the starting system, and the ID of the subnode that is the actual starting planet. This whole system like, barely works. A mixin in `MinecraftServer` calls `ServerUniverse.prepare()`, which selects a galaxy to start in, and selects a position from that galaxy to place the starting system in. We can generate universe sectors, but unfortunately, we cannot actually generate galaxy sectors, because we would need to provide the starting system generator, which we are in the process of making. So for now we just pick a random galaxy in a radius and put the starting system in sector `(0, 0, 0)`.

ok i ran out of steam writing this. maybe ill write more later.