# Avi's Unnamed Space Mod

## Code Overview
- A ton of the client-side rendering stuff is done via custom implementations of vanilla structures, like `BufferBuilder` and `RenderTarget`
- Custom math types that dont sucks so much to use: `Vec3`, `Vec3i`, `Quat`, `Color`, etc. These are located currently in `net/xavil/util/math`
- Procedural star system generate is split between `net/xavil/ultraviolet/common/universe/system/StarSystemGenerator` and the classes in `net/xavil/ultraviolet/common/universe/system/gen`. Currently, it is _horribly_, _horribly_ broken. It started as a half-baked implementation of the ancient Accrete algorithm (aka Starform or Stargen) which I tried to modify to be more flexible, but I don't know astrophysics!

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
	- Full-resolution ID that allows identifying a single celestial node, given no prior context.
- `GalaxySectorId`
	- ID that allows identifying a star system, given a galaxy as prior context.
- `SystemId`
	- ID that allows identifying a star system, given no prior context.
- `UniverseSectorId`
	- ID that allows identifying a galaxy, given no prior context.

### `GalaxySector`, `SectorManager`, `SectorTicket`, `SystemTicket`, `SectorPos`
`GalaxySector`s are octree nodes that span 10ly in each axis at the "base" level. These octrees are limited to 8 levels of depth. They primarily store basic info about star systems, like their position and context needed for more in-depth generation of the star system. Within a galaxy, `SectorPos`es are used to identify `GalaxySector`s. `GalaxySector`s are loaded, unloaded, and stored by `SectorManager`, which is responsible for keeping track of tickets that were issued by it, loading and unloading sectors/systems as needed. `SystemTicket`s load a single star system in its entirety, as well as the sector that the system resides within. `SectorTicket`s may load multiple sectors, and come in a few different flavors. They can either load a single sector given a `SectorPos`, or they can load a region of sectors around a point, given a "base radius", and a description of how that radius changes for each level higher in the octree. For both kinds of tickets, the contents of what they are loading can be modified at any point, and the sector manager will reflect that. For example, you can keep a single system ticket around, and simply change `ticket.id` to load a different system. `null` is also a valid ID, and will make the `SectorManager` unload anything that ticket has loaded. The `SectorManager` will do all its generation in a background threadpool, but it is also possible to wait until a ticket has completed being loaded.

### `UniverseSector` and `UniverseSectorManager`
Very similar to ther galaxy counterparts, just that they refer to sectors that contain galaxies instead of sectors that contain star systems, and are attached to a `Universe` instead of a `Galaxy`.

### `Galaxy`
Galaxies primarily act as a host for a `GalaxySectorManager`, but are also responsible for actually generating star system sectors. They contain a list of `GalaxyGenerationLayer`s, which each contribute a set of stars to the generating sector. Every galaxy gets a `BaseGalaxyGenerationLayer` attached to it, which does all the heavy lifting. The galaxy that the starting system resides in gets an additional `StartingSystemGalaxyGenerationLayer` that simply places the starting system in the correct location. There are also plans for adding authored systems, which would probably be its own generation layer.

### `Universe`
Like Minecraft's `Level`, `ServerLevel`, and `ClientLevel`, `Universe` is split between itself, `ServerUniverse`, and `ClientUniverse`. Like `Galaxy`, `Universe` serves as a context for generating galaxies, and also stores the results of that generation within itself. The generation process does not happen in the same way that star system generation does.

There are two types of seed: _common_ and _unique_. The _common_ seed is shared between everyone that uses the mod (this will be configurable in the future) and the _unique_ seed is unique per world. The _common_ seed drives the actual universe generation: the distribution of galaxies, the shape of those galaxies, the distribution of stars within those galaxies, and the layout of star systems. The _unique_ seed drives starting system placement. This means that for the default _common_ seed, two beings may share universe coordinates and be able to talk about the same planet/system/galaxy, even on worlds with different vanilla seeds. The reason the seeds are shared is so that the client can generate its own copy of the galaxies in the universe and the starts in a galaxy, so that the server doesn't need to stream a huve amount of data to the client when they, say, need to rebuild the background stars, or move around in the galaxy map view.
