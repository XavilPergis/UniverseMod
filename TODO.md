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
- [ ] Gravity
	- Basic movement adjustment
		- [ ] SmoothSwimmingMoveControl
		- [ ] FallingBlockEntity
		- [ ] ItemEntity
		- [ ] PrimedTnt
		- [ ] AbstractArrow
		- [ ] FishingHook
		- [ ] LlamaSpit
		- [ ] ShulkerBullet
		- [ ] ThrowableProjectile
		- [ ] AbstractMinecart
		- [ ] Boat ???
		- [ ] ExperienceOrb
		- [ ] LivingEntity
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