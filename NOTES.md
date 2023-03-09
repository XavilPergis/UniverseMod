# Scope
Things that are *not* in-scope:
- Fully-simulated Elite: Dangerous-style space flight (though this would be extremely cool)
- Super realistic star system generation (i *would* do this, but it is too hard!)

Things that *are* in-scope:
- Procedurally generated universe
- Fancy skybox that uses actually nearby star systems and planets
- Landable planets (Starbound-style: no seamless transition)
- Space stations/starships

# [E:D Galaxy Gen](https://www.youtube.com/watch?v=Vz3nhCykZNw)
- Major constituents of spiral galaxy shape:
	- galactic core/bulge
	- disc
		- thin disc, ~400 ly tall
		- thick disc, ~1000 ly tall
		- discs have exponential falloff
	- halo
		- spherical i think
- lots of dust in central plane
- high-level inputs
	- mass of galaxy
	- luminosity map
		- defines shape (we can use a prcedural density function)
	- luminosity:mass density map/region map
		- defines which galactic region a given pixel is in (core/halo/arms)
	- non-luminous dust contribution

- octree
	- 8 layers deep, 1280ly^3 -> 10ly^3
	- each layer manages a mass range
		- ie, higher-up layers generate more massive stars, deeper layers generate less massive ones.
		- generation range around the cursor is smaller near the core because there just so many stars there
	- nodes inherit leftover data from parent
	- mass, age, metallicity, type
		- metallicity -> ratio of dust to gas, "typically done with iron"
	- generates sytems until it has run out of mass or IDs

- sector generation
	- create a single primary star
	- star creation driven by system age and mass
		- different star lifecycle stages have different mass/whatever relationships
	- derived properties
		- radius
		- temperature
		- classification
		- color
		- luminosity
	- add planetary nebula if needed

- system generation
	- create main stars
	- create protoplanetary disc from leftover mass from star formation
		- generate clumping in stable zones
		- progress simulation through time!
	- find stable orbits and populate them
		- roche limit (inner stable orbit), hill sphere (outer stable orbit)
		- max stable orbit is about ~1/2 the radius of the hill sphere