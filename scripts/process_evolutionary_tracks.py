import math
import struct
import re
import itertools
from pathlib import Path

# this file is meant to process athyg_v30-1.csv.gz and athyg_v30-2.csv.gz from
# https://github.com/astronexus/ATHYG-Database/tree/fd8fa128df2ba41282a7af1ef0e5c9c22c82f51f/data
# into a more useful form for this mod

INPUT_PATH = Path('scripts/MIST_v1.2_feh_p0.00_afe_p0.0_vvcrit0.4_basic.iso')
OUTPUT_PATH = Path('src/main/resources/star_evolution_tracks.bin')

print('\x1b[31m----- processing star paths... -----\x1b[0m')

COLUMNS = ["EEP", "log10_isochrone_age_yr", "initial_mass", "star_mass", "star_mdot", "he_core_mass", "c_core_mass", "log_L", "log_LH", "log_LHe", "log_Teff", "log_R", "log_g", "surface_h1", "surface_he3", "surface_he4", "surface_c12", "surface_o16", "log_center_T", "log_center_Rho", "center_gamma", "center_h1", "center_he4", "center_c12", "phase"]

# age, initial_mass -> star_mass, log_L, log_Teff, log_R

# list(T): [count: i16, data: T[count]]
# tracks: list(isochrone)
# isochrone: [log_age: f32, entries: list(iso_entry)]
# iso_entry: [initial_mass: f32, final_mass: f32, log_luminosity: f32, log_temperature: f32, log_radius: f32, phase: i8]

class Entry(object):
	def __init__(self):
		self.eep = None
		self.log_age = None
		self.initial_mass = None
		self.current_mass = None
		self.log_L = None
		self.log_Teff = None
		self.log_R = None
		self.phase = None

class Isochrone(object):
	def __init__(self, log_age):
		self.log_age = log_age
		self.entries = []

	def append(self, entry):
		self.entries.append(entry)

grid = {}

with INPUT_PATH.open('rt') as iso:
	# for line in itertools.islice(iso, 25000):
	for line in iso:
		line = line.strip()
		if line.startswith("#") or line == "":
			continue

		columns = re.split('\\s+', line)
		res = Entry()
		res.eep = float(columns[0])
		res.log_age = float(columns[1])
		res.initial_mass = float(columns[2])
		res.current_mass = float(columns[3])
		res.log_L = float(columns[7])
		res.log_Teff = float(columns[10])
		res.log_R = float(columns[11])
		res.phase = float(columns[24])

		if res.log_age not in grid:
			grid[res.log_age] = Isochrone(res.log_age)
		grid[res.log_age].append(res)

		# if res.eep == 100:
		# 	print(f"{res.eep:.04f}, {res.log_age:.04f}, {res.initial_mass:.04f}, {res.current_mass:.04f}, {res.log_L:.04f}, {res.log_Teff:.04f}, {res.log_R:.04f}, {res.phase:.04f}")

with OUTPUT_PATH.open('wb') as out:
	isos = list(grid.values())
	isos.sort(key = lambda iso: iso.log_age)

	out.write(struct.pack('>i', len(isos)))
	for iso in isos:
		iso.entries.sort(key = lambda entry: entry.initial_mass)
		out.write(struct.pack('>fi', iso.log_age, len(iso.entries)))
		for entry in iso.entries:
			out.write(struct.pack('>fffffb', entry.initial_mass, entry.current_mass, entry.log_L, entry.log_Teff, entry.log_R, int(entry.phase)))
