# Ideas

- Chemical drives should have some good stuff going for them, so that they can always remain competitive with more advanced options like ion and EM drives
- Warp drives completely outclass subluminal options, but have a minimum drive engagement, so that subluminal drives never become irrelevant
	- subluminal warp drives cannot be used outside of supercruise
- intergalactic warp drives outclass interstellar warp drives, but have a minimum jump distance that requires interstallar drives
	- maybe like 1kly minimum jump distance
	- maybe intergalactic drives interact with interstellar drives and and provide fuel cost discounts for longer-range jumps

- interstellar infrastructure that drastically reduces fuel consumption, and allows subluminal ships to travel between star systems
	- sorta like the hyperlanes in cowboy bebop
	- good for automated logistics ships or something

- normal space
	- Pressurized Gas Nozzle
	- is there any use for this?
- intrastellar
	- local (planet-moon systems)
		- Chemical Drive
	- global (entire star system)
		- Ion Drive
		- Electromagnetic Drive
		- Warp Drive
- interstellar
	- Interstellar Warp Drive MkI
		- 0.25-5 ly range
	- Interstellar Warp Drive MkII
		- 0.75-50 ly range
	- Interstellar Warp Drive MkIII
		- 1-1000 ly range
- intergalactic
	- Intergalactic Warp Drive
		- 1k-10M ly range

- Moons of starter planet
	- no superluminal options
	- basic chemical drives
- Entire star system
	- both subluminal and superluminal options
	- ion drives (subluminal)
		- entry-level ion drives are worse than top-level chemical drives
	- EM drives (subluminal, needs advaned physics-breaking materials)
	- low-power warp drives
- Interstellar travel
	- superluminal-only from here on out
	- A few tiers with varying jump ranges from a few ly to a few thousand ly
- Intergalactic travel
	- Endgame, only one tier available

## Improvements
- System Gen:
	- heavily bias orbits of stars in myltiple-star systems to be small, so that there's an interesting mix of orbits around each star and circumbinary orbits.
	- only generate one "layer" deep at a time. ie, generate planets around a star, then find out what planets can form moons, and generate those

# Short-ish term


## Rendering
- Figure out how to properly size celestial body billboards/cubes/spheres/whatever
	- FOV needs to be taken into account
- When using the spyglass, info about the targeted celestial body should be displayed
- Custom Planetary body shader that does shadows n stuff
- [ ] Render the galaxy somehow

## Other
- [ ] Day/night cycle based on star system configuration
- [x] Server-authoratative celestial body positioning
- [ ] Spyglass with larger zoom
- [ ] Augment that shows the celestial bodies that belong to the current system
- [x] Save planet ID in server world nbt
- [ ] Don't load every dynamically-generated world on startup
- [ ] Galaxy selection screen
- [ ] Save celestial time
- [ ] Sync system node ID when dying
- [ ] Gravity
	- Basic movement adjustment
		- [ ] SmoothSwimmingMoveControl
		- [x] FallingBlockEntity
		- [x] ItemEntity
		- [x] PrimedTnt
		- [?] AbstractArrow
		- [ ] FishingHook
		- [ ] LlamaSpit
		- [ ] ShulkerBullet (what?)
		- [x] ThrowableProjectile
		- [ ] AbstractMinecart
		- [ ] Boat ???
		- [x] ExperienceOrb
		- [x] LivingEntity
	- Disable elytra flight in planets with no or thin atmospheres

# Longterm
- Logistics ships!
- Space stations/custom space ships
- In-world starmap
- Figure out mod progression
- Planet compositions and scanning
	- Maybe you have to collect data on a planet before you can land
	- Preliminary scans could show material composition (eg. what % tin the planet is made of)
	- Intensive scans could be needed to actually land on the planet
- Render sky into HDR buffer and then tonemap when copying to main buffer

## Pretty stuff to make exploration worthwhile
- Aurorae
- Ringed Worlds
- Accretion Discs
	- Black holes may or may not be accreting

# Unresolved Questions
- Since I dont want to keep world files around for planets that were barely interacted with, how do we figure out when to keep the world, and when to not?

# Done
- Implemented my own math types and converted all my code to use them
- Fixed child planes nor working right
- Bugfixes